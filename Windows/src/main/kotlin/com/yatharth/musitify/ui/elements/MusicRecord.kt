package com.yatharth.musitify.ui.elements

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/**
 * Android spaced the grooves 20 *px* apart; on a typical 2.75x phone that is
 * ≈7.3 dp. Using dp keeps the phone look independent of desktop scaling.
 */
private val GrooveSpacing = (20f / 2.75f).dp

/**
 * Spinning vinyl: black disc, 15 grooves, white label (22 % radius) and the
 * cover [art] as a centred circle of [imageSize] dp. The whole record (disc +
 * art) rotates while [playing] — 1 rev / 10 s, linear; pausing freezes it at
 * the current angle and resuming continues from there (Android PlaybackScreen).
 * Fills the available width as a square.
 */
@Composable
fun MusicRecord(
    art: Painter,
    playing: Boolean,
    modifier: Modifier = Modifier,
    imageSize: Int = 80,
) {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(playing) {
        if (playing) {
            rotation.snapTo(rotation.value % 360f)
            while (true) {
                rotation.animateTo(
                    targetValue = rotation.value + 360f,
                    animationSpec = tween(durationMillis = 10000, easing = LinearEasing)
                )
            }
        }
    }

    Box(
        modifier = modifier.graphicsLayer { rotationZ = rotation.value },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            val radius = size.minDimension / 2
            val spacing = GrooveSpacing.toPx()

            drawCircle(color = Color.Black, radius = radius)

            repeat(15) { i ->
                val r = radius - i * spacing
                if (r > 0f) {
                    drawCircle(
                        color = Color.DarkGray.copy(alpha = 0.35f),
                        radius = r,
                        style = Stroke(width = 1f)
                    )
                }
            }

            drawCircle(color = Color.White, radius = radius * 0.22f)
        }

        Image(
            painter = art,
            contentDescription = null,
            modifier = Modifier
                .size(imageSize.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

/** Convenience overload loading the art from a local file path (placeholder fallback). */
@Composable
fun MusicRecord(
    artPath: String?,
    playing: Boolean,
    modifier: Modifier = Modifier,
    imageSize: Int = 80,
) {
    MusicRecord(
        art = rememberArtPainter(artPath),
        playing = playing,
        modifier = modifier,
        imageSize = imageSize,
    )
}
