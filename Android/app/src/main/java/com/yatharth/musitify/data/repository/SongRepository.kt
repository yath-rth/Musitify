package com.yatharth.musitify.data.repository

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.yatharth.musitify.data.dao.SongDao
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.network.model.NSong
import com.yatharth.musitify.util.ConvertSong
import com.yatharth.musitify.util.SongMedia
import com.yatharth.musitify.util.SongNormalizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.IOException
import java.util.Collections.addAll
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepository @Inject constructor(
    private val songDao: SongDao,
    private val convertSong: ConvertSong,
    private val actionRepository: ActionRepository,
    @ApplicationContext private val context: Context
) {
    private val TAG = "SongRepository"

    private fun getBaseDir(): String {
        return context.getExternalFilesDir(null)?.absolutePath
            ?: throw IOException("External storage not available")
    }

    suspend fun downloadSong(
        url: String = "https://www.youtube.com/watch?v=_bSXY75R3Wc",
        downloaded_at: Long = System.currentTimeMillis(),
        songUUID: UUID = UUID.randomUUID(),
        record: Boolean = true
    ): Song? = withContext(Dispatchers.IO) {

        // Check to see if it is a new song or else return the existing instead of wasting time
        // downloading a pre-existing song
        val exists = songDao.getSongByUrl(url = url)
        if (exists != null) return@withContext exists

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        val py = Python.getInstance()
        val module = py.getModule("downloader")
        Log.d(TAG, module.toString())

        try {
            val result = module.callAttr(
                "downloadSong",
                url,
                getBaseDir()
            )
            val songPath = convertSong.invoke(result)
            val songName = result?.callAttr("get", "title")?.toString() ?: "unknown"
            val durationSeconds = result?.callAttr("get", "duration")?.toLong()
            val artist = result?.callAttr("get", "artist")?.toString() ?: "unknown"
            val durationMs = durationSeconds?.times(1000)
            val normalizedSongName = SongNormalizer.normalize(songName).normalizedTitle

            val exists = songDao.getSongByName(normalizedSongName)
            if (exists != null && exists.url.contains("legacy")) {
                songDao.updateSong(exists.copy(url = url))
                val audioFile = File(getBaseDir() + File.separator + "$songPath.mp3")
                val artFile = File(getBaseDir() + File.separator + "$songPath.webp")

                if (audioFile.exists()) audioFile.delete()
                if (artFile.exists()) artFile.delete()
                return@withContext exists.copy(url = url)
            }

            val song = Song(
                name = normalizedSongName,
                artist = artist,
                length = durationMs ?: 0,
                timeHeard = 0,
                filePath = getBaseDir() + File.separator + "$songPath.mp3",
                artPath = getBaseDir() + File.separator + "$songPath.webp",
                downloadedAt = downloaded_at,
                url = url,
                uuid = songUUID
            )
            val insertedId = songDao.addSong(song)
            val insertedSong = if (insertedId != -1L) {
                song.copy(id = insertedId.toInt())
            } else {
                songDao.getSongByUrl(song.url)
            }

            if (insertedSong != null && record) actionRepository.likedSong(insertedSong)
            insertedSong
        } catch (e: Exception) {
            Log.e(TAG, e.message ?: "Error somewhere")
            null
        }
    }

    suspend fun downloadSongsFromSync(
        nSongs: List<NSong>,
        outputDir: String = getBaseDir(),
        maxConcurrent: Int = 5,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): Map<String, Song> = withContext(Dispatchers.IO) {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        val py = Python.getInstance()
        val module = py.getModule("downloader")
        val total = nSongs.size

        val existingSongs = songDao.getSongsByUrls(urls = nSongs.map { it.url })
        val existingSongsUrls = existingSongs.map { it.url }
        val newSongs = nSongs.filter { !existingSongsUrls.contains(it.url) }

        val completedCount = AtomicInteger(0)
        val semaphore = Semaphore(maxConcurrent)

        val songMap = newSongs.map { nSong ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    try {
                        val downloadResult = module.callAttr("downloadSong", nSong.url, outputDir)
                        val songPath = convertSong.invoke(downloadResult)
                        val songName =
                            downloadResult?.callAttr("get", "title")?.toString() ?: "unknown"
                        val artist =
                            downloadResult?.callAttr("get", "artist")?.toString() ?: "unknown"
                        val durationSeconds = downloadResult?.callAttr("get", "duration")?.toLong()
                        val durationMs = durationSeconds?.times(1000)
                        val normalizedSongName = SongNormalizer.normalize(songName).normalizedTitle

                        val exists = songDao.getSongByName(normalizedSongName)
                        if (exists != null && exists.url.contains("legacy")) {
                            songDao.updateSong(exists.copy(url = nSong.url))
                            val audioFile = File(getBaseDir() + File.separator + "$songPath.mp3")
                            val artFile = File(getBaseDir() + File.separator + "$songPath.webp")

                            if (audioFile.exists()) audioFile.delete()
                            if (artFile.exists()) artFile.delete()
                            return@async Pair(nSong.id, exists.copy(url = nSong.url))
                        }

                        val song = Song(
                            name = normalizedSongName,
                            artist = artist,
                            length = durationMs ?: 0,
                            timeHeard = 0,
                            filePath = outputDir + File.separator + "$songPath.mp3",
                            artPath = outputDir + File.separator + "$songPath.webp",
                            downloadedAt = nSong.downloaded_at,
                            url = nSong.url,
                            uuid = UUID.fromString(nSong.id)
                        )

                        val insertedId = songDao.addSong(song)
                        val storedSong = if (insertedId != -1L) {
                            song.copy(id = insertedId.toInt())
                        } else {
                            songDao.getSongByUrl(song.url)
                        }
                        if (storedSong != null) Pair(nSong.id, storedSong) else null
                    } catch (e: Exception) {
                        Log.e(TAG, e.message ?: "Error somewhere")
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

        val lookup = songMap + existingSongs.associateBy { it.uuid.toString() }
        return@withContext lookup
    }

    val allSongs = songDao.getAllSongs()

    fun getFilteredSongs(query: String) = songDao.searchSongs(query)

    suspend fun deleteSongById(
        id: Int, record: Boolean = true
    ) {
        val song = songDao.getSongById(id) ?: throw IOException("Invalid song uuid")
        songDao.deleteSongById(id)
        if (record) actionRepository.unlikedSong(song.uuid.toString())

        val audioFile = File(song.filePath)
        val artFile = File(song.artPath)

        if (audioFile.exists()) audioFile.delete()
        if (artFile.exists()) artFile.delete()
    }

    suspend fun deleteSongByUUID(
        id: String, record: Boolean = true
    ) {
        val song = songDao.getSongByUuid(id) ?: throw IOException("Invalid song uuid")
        songDao.deleteSongByUuid(id)
        if (record) actionRepository.unlikedSong(song.uuid.toString())

        val audioFile = File(song.filePath)
        val artFile = File(song.artPath)

        if (audioFile.exists()) audioFile.delete()
        if (artFile.exists()) artFile.delete()
    }

    suspend fun getSongsByUrl(urls: List<String>): List<Song> {
        return songDao.getSongsByUrls(urls)
    }

    suspend fun updateSong(song: Song, record: Boolean = true) {
        if (record) {
            // Get the old song to get the old name
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

    suspend fun fetchAllAndUpdate() {
        val songs = songDao.getAllSongsWithoutLength()
        songs.forEachIndexed { index, song ->
            val length = SongMedia.getAudioDurationMs(song.filePath)
            songDao.updateLength(song.id, length)
        }
        Log.d("Temp", "Completed migration like behaviour")
    }

    suspend fun addSongFromSync(songUuid: String?, payload: String?) {
        if (songUuid == null || payload == null) throw IOException("Song uuid or payload is wrong")
        val jsonObject = Json.parseToJsonElement(payload).jsonObject
        val url = jsonObject["url"]?.jsonPrimitive?.content
            ?: throw IOException("Url not provided for song")
        val downloaded_at = jsonObject["downloaded_at"]?.jsonPrimitive?.content?.toLongOrNull()
            ?: System.currentTimeMillis()

        val downloadedSong = downloadSong(
            url = url,
            songUUID = UUID.fromString(songUuid),
            downloaded_at = downloaded_at,
            record = false
        )
        Log.d(TAG, "Downloaded song ${downloadedSong?.name}")
    }
}