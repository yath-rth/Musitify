package com.yatharth.musitify.service

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.yatharth.musitify.data.dao.SongDao
import com.yatharth.musitify.data.entity.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DownloadService @Inject constructor(
    private val songDao: SongDao
) {
    private val TAG = "DownloadService"

    suspend fun downloadSong(
        url: String = "https://www.youtube.com/watch?v=_bSXY75R3Wc",
        applicationContext: Context
    ) = withContext(Dispatchers.IO) {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(applicationContext))
        }
        val py = Python.getInstance()
        val module = py.getModule("downloader")
        Log.d(TAG, module.toString())

        val result = module.callAttr(
            "downloadSong",
            url,
            applicationContext.getExternalFilesDir(null)?.absolutePath
        )

        val inputPath = result.toString()
        val name = inputPath.substringBeforeLast(".").substringAfterLast("/")
        val outputPath = "${inputPath.substringBeforeLast(".")}.mp3"

        val ffmpegCommand = "-i \"$inputPath\" -vn -acodec libmp3lame -q:a 2 \"$outputPath\""
        Log.d(TAG, result.toString())

        val session: FFmpegSession = FFmpegKit.execute(ffmpegCommand)
        if (ReturnCode.isSuccess(session.getReturnCode())) {
            Log.d(TAG, "Converted: $outputPath")

            val webmFile = File(inputPath)
            if (webmFile.exists()) {
                val deleted = webmFile.delete()
                Log.d(TAG, "Deleted webm: $deleted")
            }

            songDao.addSong(
                Song(
                    name = name,
                    length = 60, //TODO: Implement a way to provide meta-data from yt-dlp to kotlin
                    timeHeard = 0,
                    downloadedAt = System.currentTimeMillis(),
                )
            )

            audioPlayer(
                applicationContext.getExternalFilesDir(null)?.absolutePath,
                "$name.mp3"
            )
        } else {
            Log.e(TAG, "FFmpeg failed: ${session.getAllLogsAsString()}")
        }
    }

    fun audioPlayer(path: String?, fileName: String?) {
        val mp = MediaPlayer()

        try {
            mp.setDataSource(path + File.separator + fileName)
            mp.prepare()
            mp.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}