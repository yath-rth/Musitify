package com.yatharth.musitify.data.repository

import android.content.Context
import android.util.Log
import androidx.compose.ui.input.key.Key.Companion.F
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.yatharth.musitify.data.dao.SongDao
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.util.ConvertSong
import com.yatharth.musitify.util.SongMedia
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepository @Inject constructor(
    private val songDao: SongDao,
    private val convertSong: ConvertSong,
    @ApplicationContext private val applicationContext: Context
) {
    private val TAG = "SongRepository"

    suspend fun downloadSong(
        url: String = "https://www.youtube.com/watch?v=_bSXY75R3Wc",
    ) = withContext(Dispatchers.IO) {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(applicationContext))
        }
        val py = Python.getInstance()
        val module = py.getModule("downloader")
        Log.d(TAG, module.toString())

        try {
            val result = module.callAttr(
                "downloadSong",
                url,
                applicationContext.getExternalFilesDir(null)?.absolutePath
            )
            val songName = convertSong.invoke(result)
            val durationSeconds = result["duration"]?.toLong()
            val durationMs = durationSeconds?.times(1000)

            songDao.addSong(
                Song(
                    name = songName,
                    length = durationMs ?: 0,
                    timeHeard = 0,
                    filePath = applicationContext.getExternalFilesDir(null)?.absolutePath + File.separator + "$songName.mp3",
                    artPath = applicationContext.getExternalFilesDir(null)?.absolutePath + File.separator + "$songName.webp",
                    downloadedAt = System.currentTimeMillis(),
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, e.message ?: "Error somewhere")
        }
    }

    val allSongs = songDao.getAllSongs()

    fun getFilteredSongs(query: String) = songDao.searchSongs(query)

    suspend fun deleteSong(id: Int) {
        val song = songDao.getSongById(id) ?: return
        val audioFile = File(song.filePath)
        val artFile = File(song.artPath)

        if (audioFile.exists()) audioFile.delete()
        if (artFile.exists()) artFile.delete()

        songDao.deleteSong(id)
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