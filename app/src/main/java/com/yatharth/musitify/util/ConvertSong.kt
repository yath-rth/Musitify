package com.yatharth.musitify.util

import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import com.chaquo.python.PyObject
import java.io.File
import javax.inject.Inject


class ConvertSong @Inject constructor(
) {
    private val TAG = "Convert"

    fun invoke(result: PyObject?): String {
        val inputPath = result.toString()
        val name = inputPath.substringBeforeLast(".").substringAfterLast("/")
        val outputPath = "${inputPath.substringBeforeLast(".")}.mp3"

        val ffmpegCommand = "-i \"$inputPath\" -vn -acodec libmp3lame -q:a 2 \"$outputPath\""
        Log.d(TAG, result.toString())

        val session: FFmpegSession = FFmpegKit.execute(ffmpegCommand)
        if (ReturnCode.isSuccess(session.getReturnCode())) {
            Log.d(TAG, "Converted: $outputPath")

            val webmFile = File(inputPath)
            val webmPartFile = File("$inputPath.part")
            if (webmFile.exists()) {
                val deleted = webmFile.delete()
                Log.d(TAG, "Deleted webm: $deleted")
            }
            if (webmPartFile.exists()) {
                val deleted = webmPartFile.delete()
                Log.d(TAG, "Deleted webm: $deleted")
            }
        } else {
            Log.e(TAG, "FFmpeg failed: ${session.getAllLogsAsString()}")
        }

        return name
    }

}