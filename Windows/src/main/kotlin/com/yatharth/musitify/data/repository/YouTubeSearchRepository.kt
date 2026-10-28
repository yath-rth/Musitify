package com.yatharth.musitify.data.repository

import com.yatharth.musitify.engine.Downloader
import com.yatharth.musitify.engine.ProcessFailedException
import com.yatharth.musitify.engine.ProcessRunner
import com.yatharth.musitify.engine.Tools
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import java.io.File

data class YouTubeResult(
    val id: String,
    val url: String,
    val title: String,
    val artist: String,
    val durationSeconds: Long?,
    val thumbnailUrl: String
)

/**
 * Port of Android `YouTubeSearchRepository` (`search_youtube` / `get_stream_url` in
 * downloader.py), using the bundled yt-dlp executable instead of the Python module.
 */
class YouTubeSearchRepository(
    private val ytDlp: () -> File = Tools::ytDlp,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Top YouTube matches for [query] (flat listing, nothing is downloaded). */
    suspend fun search(query: String, limit: Int = 8): List<YouTubeResult> {
        val cmd = listOf(
            ytDlp().absolutePath,
            "--flat-playlist", "-J", "-q", "--no-warnings", "--encoding", "utf-8",
            "ytsearch$limit:$query",
        )
        val result = ProcessRunner.run(cmd)
        if (!result.isSuccess) throw ProcessFailedException(cmd, result)
        return parseSearch(result.stdout)
    }

    /** Direct audio stream for a quick preview, without downloading anything. */
    suspend fun streamUrl(videoUrl: String): String {
        val cmd = listOf(
            ytDlp().absolutePath,
            "-f", "bestaudio[ext=m4a]/bestaudio/best",
            "-g", "-q", "--no-warnings", "--no-playlist", "--encoding", "utf-8",
            videoUrl,
        )
        val result = ProcessRunner.run(cmd)
        if (!result.isSuccess) throw ProcessFailedException(cmd, result)
        return result.stdout.lineSequence().map { it.trim() }.firstOrNull { it.startsWith("http") }
            ?: throw IllegalStateException("yt-dlp returned no stream URL")
    }

    internal fun parseSearch(stdout: String): List<YouTubeResult> {
        val line = stdout.lineSequence().map { it.trim() }.lastOrNull { it.startsWith("{") } ?: return emptyList()
        val info = json.parseToJsonElement(line).jsonObject
        val entries = (info["entries"] as? JsonArray).orEmpty()
        return entries.mapNotNull { e ->
            val o = e as? JsonObject ?: return@mapNotNull null
            val id = o.str("id")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            YouTubeResult(
                id = id,
                url = "https://www.youtube.com/watch?v=$id",
                title = o.str("title")?.takeIf { it.isNotBlank() } ?: "Unknown title",
                artist = Downloader.artistOf(o),
                durationSeconds = (o["duration"] as? JsonPrimitive)?.doubleOrNull?.toLong(),
                thumbnailUrl = "https://i.ytimg.com/vi/$id/mqdefault.jpg",
            )
        }
    }

    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
}
