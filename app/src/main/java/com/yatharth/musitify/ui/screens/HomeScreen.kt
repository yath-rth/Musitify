package com.yatharth.musitify.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.repository.PlayerUiState
import com.yatharth.musitify.data.viewmodel.HomeScreenViewModel
import com.yatharth.musitify.ui.elements.PlaybackControls
import com.yatharth.musitify.ui.elements.SongItemUI
import com.yatharth.musitify.ui.theme.MusitifyTheme

@Composable
fun HomeRoute(
    modifier: Modifier,
    homeScreenViewModel: HomeScreenViewModel = hiltViewModel()
) {
    val TAG = "HomeScreen"

    val songs by homeScreenViewModel.songs.collectAsState()
    val uiState by homeScreenViewModel.uiState.collectAsState()

    Column(
        modifier = modifier.padding(8.dp)
    ){
        LazyColumn(
            modifier = Modifier.padding(8.dp).weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            //TODO: Add a place to add and search for songs
            item {

            }

            items(
                count = songs.size,
                key = { index -> songs[index].id }
            ) {
                val song = songs[it]
                SongItemUI(
                    song = song,
                    onPlay = {
                        Log.d(TAG, "User trying to play song ${song.name}")
                        homeScreenViewModel.playAudio(it)
                    }
                )
            }
        }

        PlaybackControls(
            modifier = Modifier.weight(0.1f),
            uiState = uiState
        )
    }
}

