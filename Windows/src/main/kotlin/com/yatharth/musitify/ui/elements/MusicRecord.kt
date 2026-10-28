package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer

/**
 * A vinyl record with the song's cover art ([artPath]) as the centre label.
 *
 * Only the disc and label rotate ([rotationDegrees]). The light reflection is
 * drawn on top without rotating, the way light falls on a real spinning record,
 * so the motion reads as spin rather than a rotating picture.
 *
 * @param labelFraction size of the cover-art label relative to the disc.
 */
@Composable
fun MusicRecord(
    artPath: String?,
    modifier: Modifier = Modifier,
    rotationDegrees: Float = 0f,
    labelFraction: Float = 0.5f,
) {
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        // Disc + label: rotates.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotationDegrees },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2
                val labelRadius = radius * labelFraction
                drawCircle(color = Color(0xFF111114), radius = radius)

                // Grooves fill the band between the label and the edge,
                // spaced relative to the size so they look the same anywhere.
                val grooves = 14
                val band = radius - labelRadius
                repeat(grooves) { i ->
                    drawCircle(
                        color = Color.White.copy(alpha = if (i % 3 == 0) 0.10f else 0.05f),
                        radius = labelRadius + band * (i + 1) / (grooves + 1),
                        style = Stroke(width = 1.2f)
                    )
                }
                // Thin dark ring where the label meets the vinyl.
                drawCircle(
                    color = Color.Black,
                    radius = labelRadius + 2f,
                    style = Stroke(width = 4f)
                )
            }

            ArtImage(
                path = artPath,
                modifier = Modifier
                    .fillMaxWidth(labelFraction)
                    .aspectRatio(1f)
                    .clip(CircleShape)
            )

            // Spindle hole.
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension / 2
                drawCircle(color = Color(0xFF111114), radius = r * 0.035f)
            }
        }

        // Reflection: does not rotate.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.10f),
                        Color.Transparent,
                        Color.Transparent,
                        Color.White.copy(alpha = 0.06f),
                        Color.Transparent,
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                ),
                radius = radius
            )
        }
    }
}
