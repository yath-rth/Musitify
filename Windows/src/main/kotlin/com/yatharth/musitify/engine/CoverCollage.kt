package com.yatharth.musitify.engine

import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import java.io.File
import kotlin.math.roundToInt

/**
 * Port of Android `SongMedia.createPlaylistCoverBitmap` / `cachePlaylistCover` using Skia
 * (bundled with Compose Desktop, decodes WebP).
 *
 * Rules (same as Android): [outputSize]² transparent canvas; first 4 paths fill TL, TR, BL, BR in
 * that order; each source is center-cropped to a square and scaled into its quadrant; a missing
 * or undecodable path leaves its quadrant transparent (no shifting); fewer than 4 paths leave the
 * remaining quadrants transparent (no tiling, no full-bleed single image). Output is PNG.
 */
object CoverCollage {

    /** Renders the collage and returns encoded PNG bytes. */
    fun render(artPaths: List<String?>, outputSize: Int = 512): ByteArray {
        val surface = Surface.makeRasterN32Premul(outputSize, outputSize)
        try {
            val canvas = surface.canvas
            canvas.clear(0x00000000)
            val half = outputSize / 2
            val quadrants = listOf(
                Rect.makeLTRB(0f, 0f, half.toFloat(), half.toFloat()),
                Rect.makeLTRB(half.toFloat(), 0f, outputSize.toFloat(), half.toFloat()),
                Rect.makeLTRB(0f, half.toFloat(), half.toFloat(), outputSize.toFloat()),
                Rect.makeLTRB(half.toFloat(), half.toFloat(), outputSize.toFloat(), outputSize.toFloat()),
            )
            val paint = Paint().apply { isAntiAlias = true }
            artPaths.take(4).forEachIndexed { index, path ->
                val image = decode(path) ?: return@forEachIndexed
                image.use {
                    canvas.drawImageRect(
                        it,
                        centerCropSrcRect(it.width, it.height),
                        quadrants[index],
                        FilterMipmap(FilterMode.LINEAR, MipmapMode.NONE),
                        paint,
                        true,
                    )
                }
            }
            val snapshot = surface.makeImageSnapshot()
            return snapshot.use {
                it.encodeToData(EncodedImageFormat.PNG, 100)?.bytes
                    ?: throw IllegalStateException("PNG encoding failed")
            }
        } finally {
            surface.close()
        }
    }

    /** Renders the collage and writes it to [output] (parent dirs created). Returns [output]. */
    fun write(artPaths: List<String?>, output: File, outputSize: Int = 512): File {
        val bytes = render(artPaths, outputSize)
        output.absoluteFile.parentFile?.mkdirs()
        val tmp = File(output.path + ".tmp")
        tmp.writeBytes(bytes)
        if (output.exists()) output.delete()
        if (!tmp.renameTo(output)) {
            output.writeBytes(bytes)
            tmp.delete()
        }
        return output
    }

    fun centerCropSrcRect(width: Int, height: Int): Rect {
        val size = minOf(width, height)
        val left = (width - size) / 2
        val top = (height - size) / 2
        return Rect.makeXYWH(left.toFloat(), top.toFloat(), size.toFloat(), size.toFloat())
    }

    /** Android `SongMedia.playCountOf`: listened ms ÷ length ms, rounded. */
    fun playCountOf(timeHeardMs: Long, lengthMs: Long): Int =
        (if (lengthMs != 0L) timeHeardMs.toDouble() / lengthMs else 0.0).roundToInt()

    private fun decode(path: String?): Image? {
        if (path.isNullOrBlank()) return null
        val f = File(path)
        if (!f.isFile) return null
        return runCatching { Image.makeFromEncoded(f.readBytes()) }.getOrNull()
    }
}
