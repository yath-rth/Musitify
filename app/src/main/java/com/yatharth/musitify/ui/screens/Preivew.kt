package com.yatharth.musitify.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.ui.theme.MusitifyTheme

private val previewSongs = listOf(
    Song(
        id = 1,
        name = "Believer",
        length = 204,
        filePath = "/storage/emulated/0/Music/Believer.mp3",
        timeHeard = 37,
        downloadedAt = System.currentTimeMillis()
    ),
    Song(
        id = 2,
        name = "Bones",
        length = 167,
        filePath = "/storage/emulated/0/Music/Bones.mp3",
        timeHeard = 12,
        downloadedAt = System.currentTimeMillis()
    ),
    Song(
        id = 3,
        name = "Enemy",
        length = 173,
        filePath = "/storage/emulated/0/Music/Enemy.mp3",
        timeHeard = 84,
        downloadedAt = System.currentTimeMillis()
    ),
    Song(
        id = 4,
        name = "Radioactive",
        length = 186,
        filePath = "/storage/emulated/0/Music/Radioactive.mp3",
        timeHeard = 5,
        downloadedAt = System.currentTimeMillis()
    )
)

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
//    MusitifyTheme {
//        HomeScreen(
//            modifier = Modifier,
//            songs = previewSongs,
//            onPlayClick = {
//
//            }
//        )
//    }
}