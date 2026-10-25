package com.yatharth.musitify.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.ui.elements.MusicRecord
import com.yatharth.musitify.ui.elements.ShadowBox

// STUB — placeholder until the PlaybackScreen port lands (drop on merge).
@Composable
fun PlaybackScreen(
    modifier: Modifier = Modifier,
    song: Song,
    imageSize: Int = 100,
    playing: Boolean
) {
    ShadowBox(modifier.padding(16.dp)) {
        MusicRecord(artPath = song.artPath, playing = playing, imageSize = imageSize, modifier = Modifier.padding(16.dp))
    }
}
