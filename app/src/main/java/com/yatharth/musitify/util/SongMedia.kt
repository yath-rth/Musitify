package com.yatharth.musitify.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import java.io.File
import java.io.FileOutputStream


object SongMedia {

    fun createPlaylistCoverBitmap(
        artPaths: List<String>,
        outputSize: Int = 512 // final image is outputSize x outputSize
    ): Bitmap {
        val result = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val half = outputSize / 2

        // Quadrant positions: top-left, top-right, bottom-left, bottom-right
        val quadrants = listOf(
            Rect(0, 0, half, half),
            Rect(half, 0, outputSize, half),
            Rect(0, half, half, outputSize),
            Rect(half, half, outputSize, outputSize)
        )

        val paths = artPaths.take(4) // only use first 4

        paths.forEachIndexed { index, path ->
            val bitmap = BitmapFactory.decodeFile(path) ?: return@forEachIndexed
            val destRect = quadrants[index]
            // srcRect = whole source bitmap, scaled/cropped to fit destRect
            val srcRect = centerCropSrcRect(bitmap)
            canvas.drawBitmap(bitmap, srcRect, destRect, null)
            bitmap.recycle()
        }

        return result
    }

    fun centerCropSrcRect(bitmap: Bitmap): Rect {
        val size = minOf(bitmap.width, bitmap.height)
        val left = (bitmap.width - size) / 2
        val top = (bitmap.height - size) / 2
        return Rect(left, top, left + size, top + size)
    }

    fun cachePlaylistCover(
        context: Context,
        playlistId: Int,
        artPaths: List<String>
    ): File {
        val bitmap = createPlaylistCoverBitmap(artPaths)
        val coversDir = File(context.filesDir, "playlist_covers").apply { mkdirs() }
        val outFile = File(coversDir, "playlist_$playlistId.png")
        FileOutputStream(outFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        return outFile
    }

    fun getAudioDurationMs(filePath: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            retriever.release()
        }
    }

}