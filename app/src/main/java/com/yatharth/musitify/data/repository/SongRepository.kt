package com.yatharth.musitify.data.repository

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.yatharth.musitify.data.dao.SongDao
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.util.ConvertSong
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
            songDao.addSong(
                Song(
                    name = songName,
                    length = 60, //TODO: Implement a way to provide meta-data from yt-dlp to kotlin
                    timeHeard = 0,
                    filePath = applicationContext.getExternalFilesDir(null)?.absolutePath + File.separator + "$songName.mp3",
                    downloadedAt = System.currentTimeMillis(),
                )
            )
//            audioPlayer.playAudio(
//                applicationContext.getExternalFilesDir(null)?.absolutePath,
//                "$songName.mp3"
//            )
        } catch (e: Exception) {
            Log.e(TAG, e.message ?: "Error somewhere")
        }
    }

    val allSongs = songDao.getAllSongs()

}