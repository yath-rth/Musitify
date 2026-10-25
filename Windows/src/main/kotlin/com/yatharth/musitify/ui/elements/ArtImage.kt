@file:Suppress("DEPRECATION")

package com.yatharth.musitify.ui.elements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import java.io.File

private const val ART_PLACEHOLDER = "drawable/art_placeholder.png"

/** The bundled placeholder art (classpath resource). */
@Composable
fun artPlaceholderPainter(): Painter = painterResource(ART_PLACEHOLDER)

private fun artModel(path: String?): File? = path?.takeIf { it.isNotBlank() }?.let(::File)

/**
 * Shows the local image at [path] (thumbnails are .webp; Skia decodes them).
 * Falls back to the placeholder while loading, on error, or when [path] is
 * null/blank.
 */
@Composable
fun ArtImage(
    path: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val placeholder = artPlaceholderPainter()
    val model = remember(path) { artModel(path) }
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        placeholder = placeholder,
        error = placeholder,
        fallback = placeholder,
        contentScale = contentScale,
    )
}

/** Painter variant of [ArtImage] for custom drawing (e.g. [MusicRecord]). */
@Composable
fun rememberArtPainter(path: String?): Painter {
    val placeholder = artPlaceholderPainter()
    val model = remember(path) { artModel(path) }
    return rememberAsyncImagePainter(
        model = model,
        placeholder = placeholder,
        error = placeholder,
        fallback = placeholder,
    )
}
