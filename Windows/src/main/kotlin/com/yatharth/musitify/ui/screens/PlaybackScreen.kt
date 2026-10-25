package com.yatharth.musitify.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.ui.elements.MusicRecord
import com.yatharth.musitify.ui.elements.ShadowBox

/**
 * Maximum width of the vinyl card. Android filled the phone width (~360–420 dp);
 * on desktop the card is capped so a wide window or bottom strip does not get a
 * giant disc, and centred in whatever width [modifier] gives it. Inside a ~420 dp
 * side panel it fills the panel exactly like on a phone.
 */
private val MaxRecordCardWidth = 420.dp

/**
 * Spinning vinyl card (1:1 with Android): ShadowBox (4/4 shadow, 2 dp border,
 * 5 % corners) with 16 dp outer and inner padding around a [MusicRecord] whose
 * centred cover art is [imageSize] dp. The record turns once per 10 s while
 * [playing]; pausing freezes it at the current angle and resuming continues
 * (the Animatable loop lives in [MusicRecord]).
 */
@Composable
fun PlaybackScreen(
    modifier: Modifier = Modifier,
    song: Song,
    imageSize: Int = 100,
    playing: Boolean
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        ShadowBox(
            modifier = Modifier
                .widthIn(max = MaxRecordCardWidth)
                .fillMaxWidth()
                .padding(16.dp),
            shadowOffsetX = 4.dp,
            shadowOffsetY = 4.dp,
            borderThickness = 2.dp,
            rounding = 5
        ) {
            MusicRecord(
                artPath = song.artPath,
                playing = playing,
                imageSize = imageSize,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}
