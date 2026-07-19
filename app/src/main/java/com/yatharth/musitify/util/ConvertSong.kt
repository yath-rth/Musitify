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
        val inputPath = result?.callAttr("get", "audio")?.toString()
        if (inputPath == null) {
            Log.e(TAG, "No audio path in result: $result")
            return ""
        }

        val name = inputPath.substringBeforeLast(".").substringAfterLast("/")
        val outputPath = "${inputPath.substringBeforeLast(".")}.mp3"

        // Wait for the file to actually be ready on disk
        val inputFile = File(inputPath)
        var waited = 0
        while (!inputFile.exists() || inputFile.length() == 0L) {
            if (waited > 5000) {
                Log.e(TAG, "Timed out waiting for file: $inputPath")
                return ""
            }
            Thread.sleep(200)
            waited += 200
        }

        val ffmpegCommand = "-i \"$inputPath\" -vn -acodec libmp3lame -q:a 0 \"$outputPath\""
        Log.d(TAG, "Running ffmpeg on: $inputPath (exists=${inputFile.exists()}, size=${inputFile.length()})")

        val session: FFmpegSession = FFmpegKit.execute(ffmpegCommand)
        if (ReturnCode.isSuccess(session.getReturnCode())) {
            Log.d(TAG, "Converted: $outputPath")
            inputFile.delete()
            File("$inputPath.part").let { if (it.exists()) it.delete() }
        } else {
            Log.e(TAG, "FFmpeg failed: ${session.getAllLogsAsString()}")
        }

        return name
    }

}