package com.yatharth.musitify.data.repository

import com.yatharth.musitify.data.dao.SongDao
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.engine.DownloadedSong
import com.yatharth.musitify.engine.EnginePaths
import com.yatharth.musitify.engine.SongDownloader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Minimal song reference used by [SongRepository.downloadSongsFromSync]. Android takes
 * `network.model.NSong` (id = song uuid, url, downloaded_at); the sync port maps NSong to this.
 */
data class SyncSongRef(
    val id: String,
    val url: String,
    val downloadedAt: Long = System.currentTimeMillis(),
)

/**
 * Port of Android `SongRepository`. Chaquopy/Python is replaced by the engine [SongDownloader]
 * (yt-dlp + ffmpeg); songs land in [EnginePaths.songsDir]. User-initiated downloads go through
 * the engine `DownloadQueue`, which persists via [persistDownloadedSong] (see [LibraryDownloadAdapter]).
 */
class SongRepository(
    private val songDao: SongDao,
    private val actionRepository: ActionRepository,
    private val downloader: SongDownloader,
    private val enginePaths: EnginePaths,
) {
    private val tag = "SongRepository"

    private fun getBaseDir(): File = enginePaths.songsDir.apply { if (!isDirectory) mkdirs() }

    /**
     * Stores an already downloaded song exactly like Android's `downloadSong` after the Python call:
     * legacy-name merge (updates url, deletes the fresh files, no action) or insert + SONG_LIKED.
     */
    suspend fun persistDownloadedSong(
        downloaded: DownloadedSong,
        downloadedAt: Long = System.currentTimeMillis(),
        songUUID: UUID = UUID.randomUUID(),
        record: Boolean = true,
    ): Song? {
        val existsByUrl = songDao.getSongByUrl(downloaded.url)
        if (existsByUrl != null) return existsByUrl

        val exists = songDao.getSongByName(downloaded.normalizedName)
        if (exists != null && exists.url.contains("legacy")) {
            songDao.updateSong(exists.copy(url = downloaded.url))
            deleteFiles(downloaded.audioPath, downloaded.artPath ?: webpFor(downloaded.audioPath))
            return exists.copy(url = downloaded.url)
        }

        val song = Song(
            name = downloaded.normalizedName,
            artist = downloaded.artist,
            length = downloaded.durationMs,
            timeHeard = 0,
            filePath = downloaded.audioPath,
            // Android always stores "<base>.webp", even when no thumbnail was written.
            artPath = downloaded.artPath ?: webpFor(downloaded.audioPath),
            downloadedAt = downloadedAt,
            url = downloaded.url,
            uuid = songUUID
        )
        val insertedId = songDao.addSong(song)
        val insertedSong = if (insertedId != -1L) {
            song.copy(id = insertedId.toInt())
        } else {
            songDao.getSongByUrl(song.url)
        }

        if (insertedSong != null && record) actionRepository.likedSong(insertedSong)
        return insertedSong
    }

    suspend fun downloadSong(
        url: String = "https://www.youtube.com/watch?v=_bSXY75R3Wc",
        downloaded_at: Long = System.currentTimeMillis(),
        songUUID: UUID = UUID.randomUUID(),
        record: Boolean = true
    ): Song? = withContext(Dispatchers.IO) {
        // Existing song: return it instead of downloading again.
        val exists = songDao.getSongByUrl(url = url)
        if (exists != null) return@withContext exists

        try {
            val downloaded = downloader.downloadSong(url, getBaseDir())
            persistDownloadedSong(downloaded, downloaded_at, songUUID, record)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            System.err.println("$tag: ${e.message ?: "Error somewhere"}")
            null
        }
    }

    /**
     * Port of Android `downloadSongsFromSync`: downloads the songs not yet in the library (up to
     * [maxConcurrent] at once), always records SONG_LIKED for every newly obtained song, and
     * returns uuid-string → Song for new + pre-existing songs.
     */
    suspend fun downloadSongsFromSync(
        nSongs: List<SyncSongRef>,
        outputDir: File = getBaseDir(),
        maxConcurrent: Int = 5,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): Map<String, Song> = withContext(Dispatchers.IO) {
        val total = nSongs.size

        val existingSongs = songDao.getSongsByUrls(urls = nSongs.map { it.url })
        val existingSongsUrls = existingSongs.map { it.url }.toSet()
        val newSongs = nSongs.filter { it.url !in existingSongsUrls }

        val completedCount = AtomicInteger(0)
        val semaphore = Semaphore(maxConcurrent)

        val songMap = newSongs.map { nSong ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    try {
                        val downloaded = downloader.downloadSong(nSong.url, outputDir)
                        val stored = persistDownloadedSong(
                            downloaded,
                            downloadedAt = nSong.downloadedAt,
                            songUUID = UUID.fromString(nSong.id),
                            record = false
                        )
                        if (stored != null) Pair(nSong.id, stored) else null
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        System.err.println("$tag: ${e.message ?: "Error somewhere"}")
                        null
                    } finally {
                        val done = completedCount.incrementAndGet()
                        onProgress(done, total)
                    }
                }
            }
        }.awaitAll()
            .filterNotNull()
            .toMap()

        actionRepository.likedSongs(songMap.values.toList())

        songMap + existingSongs.associateBy { it.uuid.toString() }
    }

    val allSongs: Flow<List<Song>> = songDao.getAllSongs()

    fun getFilteredSongs(query: String): Flow<List<Song>> = songDao.searchSongs(query)

    suspend fun deleteSongById(
        id: Int, record: Boolean = true
    ) {
        val song = songDao.getSongById(id) ?: throw IOException("Invalid song uuid")
        songDao.deleteSongById(id)
        if (record) actionRepository.unlikedSong(song.uuid.toString())
        deleteFiles(song.filePath, song.artPath)
    }

    suspend fun deleteSongByUUID(
        id: String, record: Boolean = true
    ) {
        val song = songDao.getSongByUuid(id) ?: throw IOException("Invalid song uuid")
        songDao.deleteSongByUuid(id)
        if (record) actionRepository.unlikedSong(song.uuid.toString())
        deleteFiles(song.filePath, song.artPath)
    }

    suspend fun getSongsByUrl(urls: List<String>): List<Song> {
        return songDao.getSongsByUrls(urls)
    }

    suspend fun updateSong(song: Song, record: Boolean = true) {
        if (record) {
            val oldSong = songDao.getSongById(song.id)
            if (oldSong != null && oldSong.name != song.name) {
                actionRepository.recordSongRenamed(
                    song.uuid?.toString(),
                    oldSong.name,
                    song.name
                )
            }
        }
        songDao.updateSong(song)
    }

    /**
     * Android backfills `length` via MediaMetadataRetriever. Desktop: [durationOf] (e.g. ffprobe);
     * not called anywhere, as on Android.
     */
    suspend fun fetchAllAndUpdate(durationOf: (String) -> Long) {
        val songs = songDao.getAllSongsWithoutLength()
        songs.forEach { song ->
            songDao.updateLength(song.id, durationOf(song.filePath))
        }
    }

    suspend fun addSongFromSync(songUuid: String?, payload: String?) {
        if (songUuid == null || payload == null) throw IOException("Song uuid or payload is wrong")
        val jsonObject = Json.parseToJsonElement(payload).jsonObject
        val url = jsonObject["url"]?.jsonPrimitive?.content
            ?: throw IOException("Url not provided for song")
        val downloadedAt = jsonObject["downloaded_at"]?.jsonPrimitive?.content?.toLongOrNull()
            ?: System.currentTimeMillis()

        downloadSong(
            url = url,
            songUUID = UUID.fromString(songUuid),
            downloaded_at = downloadedAt,
            record = false
        )
    }

    private fun webpFor(audioPath: String): String =
        audioPath.substringBeforeLast('.', audioPath) + ".webp"

    private fun deleteFiles(vararg paths: String?) {
        paths.filterNotNull().filter { it.isNotBlank() }.forEach { p ->
            val f = File(p)
            if (f.exists()) f.delete()
        }
    }
}
