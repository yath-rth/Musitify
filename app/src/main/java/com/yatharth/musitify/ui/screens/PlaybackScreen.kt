package com.yatharth.musitify.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.ui.elements.MusicRecord
import com.yatharth.musitify.ui.elements.ShadowBox
import java.util.UUID

@Composable
fun PlaybackScreen(
    modifier: Modifier = Modifier,
    song: Song,
    imageSize: Int = 85,
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

    ShadowBox(
        modifier = modifier.padding(16.dp),
        shadowOffsetX = 4.dp,
        shadowOffsetY = 4.dp,
        borderThickness = 2.dp,
        rounding = 5
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .graphicsLayer {
                    rotationZ = rotation
                },
            contentAlignment = Alignment.Center
        ) {
            MusicRecord(
                song = song,
                imageSize = imageSize
            )
        }
    }
}

@Preview
@Composable
fun PlaybackScreenPreview() {
    PlaybackScreen(
        song = Song(
            id = 1,
            name = "Blinding Lights",
            length = 200_000L,
            filePath = "",
            artPath = "",
            timeHeard = 0,
            downloadedAt = 0L,
            url = "",
            artist = "The Weeknd",
            uuid = UUID.randomUUID()
        )
    )
}