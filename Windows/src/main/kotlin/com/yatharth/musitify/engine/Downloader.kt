package com.yatharth.musitify.engine

import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext

/** Result of [Downloader.fetchPlaylist] (port of `get_playlist_urls`). */
data class PlaylistInfo(val name: String, val urls: List<String>)

/** Result of [Downloader.downloadSong] (port of `downloadSong` + `ConvertSong`). */
data class DownloadedSong(
    val url: String,
    /** Raw title from yt-dlp. */
    val title: String,
    /** `SongNormalizer.normalize(title).normalizedTitle` — what Android stores as `Song.name`. */
    val normalizedName: String,
    val artist: String,
    /** 0 when yt-dlp reported no duration. */
    val durationMs: Long,
    /** Absolute path of the `.mp3`. */
    val audioPath: String,
    /** Absolute path of the thumbnail (`<base>.webp`), or null when none was written. */
    val artPath: String?,
)

/** Abstraction over [Downloader] so [DownloadQueue] can be tested / swapped. */
interface SongDownloader {
    suspend fun fetchPlaylist(playlistUrl: String): PlaylistInfo
    suspend fun downloadSong(url: String, outputDir: File, onProgress: ((Float) -> Unit)? = null): DownloadedSong
}

class DownloadException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * yt-dlp / ffmpeg subprocess pipeline mirroring Android `downloader.py` + `ConvertSong`.
 *
 * Steps for a song (same as Android):
 *  1. `yt-dlp -f bestaudio/best --write-thumbnail -o "%(title)s-%(uploader)s-%(release_date)s.%(ext)s"`
 *     (native container, e.g. .webm/.m4a), 3 attempts with 3 s between them.
 *  2. `ffmpeg -i <audio> -vn -acodec libmp3lame -q:a 0 <base>.mp3` (verbatim Android args), then the
 *     original + `.part` are deleted.
 *
 * Choice: the separate ffmpeg call is kept (rather than `yt-dlp -x --audio-format mp3`) so the
 * encoder arguments are byte-for-byte those of Android. Differences from Android:
 *  - `--no-playlist` (Android always passed single-video URLs anyway).
 *  - `--convert-thumbnails webp` so the art really is `<base>.webp` (Android assumed it).
 *  - Artist fallback is fixed: artist → artists (", "-joined) → uploader → "Unknown Artist".
 *  - Any failure (yt-dlp, ffmpeg, missing output) throws instead of returning a half result.
 *  - Coroutine cancellation force-kills the running subprocess.
 */
class Downloader(
    private val ytDlp: () -> File = Tools::ytDlp,
    private val ffmpeg: () -> File = Tools::ffmpeg,
    private val retries: Int = 3,
    private val retryDelayMs: Long = 3_000,
) : SongDownloader {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Flat playlist listing. No retry (same as Android). */
    override suspend fun fetchPlaylist(playlistUrl: String): PlaylistInfo {
        val cmd = listOf(
            ytDlp().absolutePath,
            "--flat-playlist", "-J", "-q", "--no-warnings", "--encoding", "utf-8",
            playlistUrl.trim(),
        )
        val result = ProcessRunner.run(cmd)
        if (!result.isSuccess) throw ProcessFailedException(cmd, result)
        val info = parseInfo(result.stdout)
        val entries = (info["entries"] as? JsonArray).orEmpty()
        val urls = entries.mapNotNull { e ->
            val o = e as? JsonObject ?: return@mapNotNull null
            o.str("url") ?: o.str("webpage_url")
        }
        val name = info.str("title")?.trim()?.takeIf { it.isNotEmpty() } ?: "Unknown Playlist"
        return PlaylistInfo(name, urls)
    }

    /**
     * Downloads [url] into [outputDir] producing `<base>.mp3` and `<base>.webp`.
     * @param onProgress optional 0..1 download progress (from yt-dlp), called on a reader thread.
     * @throws DownloadException / ProcessFailedException on failure, CancellationException on cancel.
     */
    override suspend fun downloadSong(
        url: String,
        outputDir: File,
        onProgress: ((Float) -> Unit)?,
    ): DownloadedSong {
        outputDir.mkdirs()
        var lastError: Exception? = null
        var info: JsonObject? = null
        for (attempt in 0 until retries) {
            try {
                info = runYtDlp(url.trim(), outputDir, onProgress)
                break
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                if (attempt < retries - 1) delay(retryDelayMs)
            }
        }
        info ?: throw (lastError ?: DownloadException("yt-dlp failed for $url"))

        val audio = audioPathOf(info) ?: throw DownloadException("yt-dlp did not report an output file for $url")
        val base = audio.substringBeforeLast('.')
        val mp3 = convertToMp3(File(audio))

        val thumbnail = listOf(".webp", ".jpg", ".png").map { File(base + it) }.firstOrNull { it.isFile }
        val title = info.str("title") ?: "unknown"
        val durationSec = (info["duration"] as? JsonPrimitive)?.doubleOrNull
        return DownloadedSong(
            url = url.trim(),
            title = title,
            normalizedName = SongNormalizer.normalize(title).normalizedTitle,
            artist = artistOf(info),
            durationMs = durationSec?.let { (it * 1000).toLong() } ?: 0L,
            audioPath = mp3.absolutePath,
            artPath = thumbnail?.absolutePath,
        )
    }

    private suspend fun runYtDlp(url: String, outputDir: File, onProgress: ((Float) -> Unit)?): JsonObject {
        val ff = runCatching { ffmpeg() }.getOrNull()
        val cmd = buildList {
            add(ytDlp().absolutePath)
            add("--no-playlist")
            addAll(listOf("-f", "bestaudio/best"))
            add("--write-thumbnail")
            if (ff != null) {
                addAll(listOf("--convert-thumbnails", "webp"))
                addAll(listOf("--ffmpeg-location", ff.absolutePath))
            }
            add("--no-warnings")
            addAll(listOf("-P", outputDir.absolutePath))
            addAll(listOf("-o", "%(title)s-%(uploader)s-%(release_date)s.%(ext)s"))
            addAll(listOf("-q", "--no-simulate", "-J"))
            if (onProgress != null) {
                addAll(listOf("--progress", "--newline", "--progress-template", "download:$PROGRESS_TAG%(progress._percent_str)s"))
            }
            addAll(listOf("--encoding", "utf-8", "--no-colors"))
            add(url)
        }
        val progressLine: ((String) -> Unit)? = onProgress?.let { cb ->
            { line ->
                val i = line.indexOf(PROGRESS_TAG)
                if (i >= 0) {
                    line.substring(i + PROGRESS_TAG.length).trim().removeSuffix("%").trim()
                        .toFloatOrNull()?.let { cb((it / 100f).coerceIn(0f, 1f)) }
                }
            }
        }
        val result = ProcessRunner.run(cmd, onStdoutLine = progressLine, onStderrLine = progressLine)
        if (!result.isSuccess) throw ProcessFailedException(cmd, result)
        return parseInfo(result.stdout)
    }

    /** ffmpeg conversion, verbatim Android args. Deletes the source + `.part` on success. */
    private suspend fun convertToMp3(input: File): File {
        val output = File(input.path.substringBeforeLast('.') + ".mp3")
        if (input.extension.equals("mp3", ignoreCase = true)) {
            if (input.isFile && input.length() > 0) return input
            throw DownloadException("Downloaded file missing: ${input.path}")
        }
        // Wait for the input file (200 ms × 5 s, as Android)
        val start = System.currentTimeMillis()
        while (!input.isFile || input.length() == 0L) {
            coroutineContext.ensureActive()
            if (System.currentTimeMillis() - start > 5_000) throw DownloadException("Downloaded file missing: ${input.path}")
            delay(200)
        }
        val cmd = listOf(
            ffmpeg().absolutePath, "-hide_banner", "-nostdin", "-y",
            "-i", input.absolutePath, "-vn", "-acodec", "libmp3lame", "-q:a", "0",
            output.absolutePath,
        )
        val result = try {
            ProcessRunner.run(cmd)
        } catch (e: CancellationException) {
            output.delete()
            throw e
        }
        if (!result.isSuccess || !output.isFile || output.length() == 0L) {
            output.delete()
            throw ProcessFailedException(cmd, result)
        }
        input.delete()
        File(input.path + ".part").delete()
        return output
    }

    /** Duration of an audio file via ffprobe (replacement for MediaMetadataRetriever). 0 on failure. */
    suspend fun probeDurationMs(path: String): Long {
        val probe = Tools.find(Tools.FFPROBE) ?: return 0
        val result = runCatching {
            ProcessRunner.run(
                listOf(
                    probe.absolutePath, "-v", "error", "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1", path,
                )
            )
        }.getOrElse { if (it is CancellationException) throw it; return 0 }
        return result.stdout.trim().lines().firstOrNull()?.toDoubleOrNull()?.let { (it * 1000).toLong() } ?: 0
    }

    private fun parseInfo(stdout: String): JsonObject {
        val line = stdout.lineSequence().map { it.trim() }.lastOrNull { it.startsWith("{") }
            ?: throw DownloadException("yt-dlp produced no JSON output")
        return json.parseToJsonElement(line).jsonObject
    }

    companion object {
        private const val PROGRESS_TAG = "MUSITIFY_PROGRESS "

        /** Fixed Android fallback chain: artist → artists → uploader → "Unknown Artist". Never "None". */
        fun artistOf(info: JsonObject): String {
            info.str("artist")?.takeIf { it.isNotBlank() }?.let { return it }
            (info["artists"] as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf { s -> s.isNotBlank() } }
                ?.takeIf { it.isNotEmpty() }
                ?.let { return it.joinToString(", ") }
            info.str("uploader")?.takeIf { it.isNotBlank() }?.let { return it }
            return "Unknown Artist"
        }

        internal fun audioPathOf(info: JsonObject): String? {
            val rd = (info["requested_downloads"] as? JsonArray)?.firstOrNull() as? JsonObject
            return rd?.str("filepath") ?: rd?.str("_filename") ?: info.str("_filename") ?: info.str("filename")
        }

        private fun JsonObject.str(key: String): String? = when (val e: JsonElement? = this[key]) {
            null, JsonNull -> null
            is JsonPrimitive -> e.contentOrNull
            else -> null
        }
    }
}
