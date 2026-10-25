package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yatharth.musitify.R
import com.yatharth.musitify.data.entity.Song

@Composable
fun MusicRecord(
    modifier: Modifier = Modifier,
    song: Song,
    imageSize: Int = 80,
) {
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
    ) {
        val radius = size.minDimension / 2

        drawCircle(
            color = Color.Black,
            radius = radius
        )

        repeat(15) { i ->
            drawCircle(
                color = Color.DarkGray.copy(alpha = 0.35f),
                radius = radius - i * 20f,
                style = Stroke(width = 1f)
            )
        }

        drawCircle(
            color = Color.White,
            radius = radius * 0.22f
        )
    }

    if (LocalInspectionMode.current) {
        Image(
            painter = painterResource(R.drawable.art_placeholder),
            contentDescription = null,
            modifier = Modifier
                .size(imageSize.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        AsyncImage(
            model = song.artPath,
            contentDescription = null,
            modifier = Modifier
                .size(imageSize.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}