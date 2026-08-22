package com.yatharth.musitify.data.repository

import android.content.Context
import android.util.Log
import coil.util.CoilUtils.result
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
import java.io.File
import java.util.UUID
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

    suspend fun downloadSong(
        url: String = "https://www.youtube.com/watch?v=_bSXY75R3Wc",
    ): Song? = withContext(Dispatchers.IO) {
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
                context.getExternalFilesDir(null)?.absolutePath
            )
            val songPath = convertSong.invoke(result)
            var songName = result?.callAttr("get", "title")?.toString() ?: "unknown"
            val durationSeconds = result?.callAttr("get", "duration")?.toLong()
            val artist = result?.callAttr("get", "artist")?.toString() ?: "unknown"
            val durationMs = durationSeconds?.times(1000)

            songName = SongNormalizer.normalize(songName).normalizedTitle

            val song = Song(
                name = songName,
                artist = artist,
                length = durationMs ?: 0,
                timeHeard = 0,
                filePath = context.getExternalFilesDir(null)?.absolutePath + File.separator + "$songPath.mp3",
                artPath = context.getExternalFilesDir(null)?.absolutePath + File.separator + "$songPath.webp",
                downloadedAt = System.currentTimeMillis(),
                url = url,
                uuid = UUID.randomUUID()
            )
            val insertedId = songDao.addSong(song)
            val insertedSong = if (insertedId != -1L) {
                song.copy(id = insertedId.toInt())
            } else {
                songDao.getSongByUrl(song.url) ?: song
            }

            actionRepository.likedSong(insertedSong)
            insertedSong
        } catch (e: Exception) {
            Log.e(TAG, e.message ?: "Error somewhere")
            null
        }
    }

    suspend fun downloadSongsFromSync(
        nSongs: List<NSong>,
        outputDir: String? = context.getExternalFilesDir(null)?.absolutePath,
        maxConcurrent: Int = 5,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): Map<String, Song> = withContext(Dispatchers.IO) {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        val py = Python.getInstance()
        val module = py.getModule("downloader")
        val total = nSongs.size

        val completedCount = java.util.concurrent.atomic.AtomicInteger(0)
        val semaphore = Semaphore(maxConcurrent)

        val songMap = nSongs.map { nSong ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    try {
                        val downloadResult = module.callAttr("downloadSong", nSong.url, outputDir)
                        var songName = convertSong.invoke(downloadResult)
                        val artist =
                            downloadResult?.callAttr("get", "artist")?.toString() ?: "unknown"
                        val durationSeconds = downloadResult?.callAttr("get", "duration")?.toLong()
                        val durationMs = durationSeconds?.times(1000)
                        songName = SongNormalizer.normalize(songName).normalizedTitle

                        val song = Song(
                            name = songName,
                            artist = artist,
                            length = durationMs ?: 0,
                            timeHeard = 0,
                            filePath = context.getExternalFilesDir(null)?.absolutePath + File.separator + "$songName.mp3",
                            artPath = context.getExternalFilesDir(null)?.absolutePath + File.separator + "$songName.webp",
                            downloadedAt = nSong.downloaded_at,
                            url = nSong.url,
                            uuid = UUID.fromString(nSong.id)
                        )

                        val insertedId = songDao.addSong(song)
                        val storedSong = if (insertedId != -1L) {
                            song.copy(id = insertedId.toInt())
                        } else {
                            // Insert was ignored due to a unique-constraint conflict
                            // (url/uuid already present) -> fetch the existing row
                            songDao.getSongByUrl(song.url) ?: song
                        }
                        Pair(nSong.id, storedSong)
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
        songMap
    }

    val allSongs = songDao.getAllSongs()

    fun getFilteredSongs(query: String) = songDao.searchSongs(query)

    suspend fun deleteSong(id: Int) {
        val song = songDao.getSongById(id) ?: return
        val audioFile = File(song.filePath)
        val artFile = File(song.artPath)

        if (audioFile.exists()) audioFile.delete()
        if (artFile.exists()) artFile.delete()

        actionRepository.unlikedSong(song.uuid.toString())
        songDao.deleteSong(id)
    }

    suspend fun getSongsByUrl(urls: List<String>): List<Song> {
        return songDao.getSongsByUrls(urls)
    }

    suspend fun fetchAllAndUpdate() {
        val songs = songDao.getAllSongsWithoutLength()
        songs.forEachIndexed { index, song ->
            val length = SongMedia.getAudioDurationMs(song.filePath)
            songDao.updateLength(song.id, length)
        }
        Log.d("Temp", "Completed migration like behaviour")
    }
}