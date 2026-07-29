package com.yatharth.musitify.ui.screens

import android.R.attr.contentDescription
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.yatharth.musitify.data.entity.Song

@Composable
fun PlaybackScreen(
    song: Song
) {
    val infinite = rememberInfiniteTransition()

    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(
                durationMillis = 6000,
                easing = LinearEasing
            )
        )
    )

    Box(
        modifier = Modifier.graphicsLayer {
            rotationZ = rotation
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(280.dp)
        ) {
            val radius = size.minDimension / 2

            drawCircle(
                color = Color.Black,
                radius = radius
            )

            repeat(15) { i ->
                drawCircle(
                    color = Color.DarkGray.copy(alpha = 0.35f),
                    radius = radius - i * 8f,
                    style = Stroke(width = 1f)
                )
            }

            drawCircle(
                color = Color.Gray,
                radius = radius * 0.22f
            )
        }

        AsyncImage(
            model = song.artPath,
            contentDescription = null,
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}