package com.yatharth.musitify.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yatharth.musitify.data.entity.Playlist
import com.yatharth.musitify.data.viewmodel.AddSongsViewModel
import com.yatharth.musitify.data.viewmodel.HomeScreenViewModel
import com.yatharth.musitify.data.viewmodel.PlaybackControllerViewModel
import com.yatharth.musitify.ui.elements.AddSongDialog
import com.yatharth.musitify.ui.elements.CustomButton
import com.yatharth.musitify.ui.elements.PlaybackControls
import com.yatharth.musitify.ui.elements.PlaylistDialog
import com.yatharth.musitify.ui.elements.SongItemUI
import com.yatharth.musitify.ui.theme.designSystemButton
import com.yatharth.musitify.ui.theme.text

//TODO: Implement a way to display both playlists and songs separately
//TODO: A screen to show the songs contained inside a playlist
@Composable
fun HomeScreen(
    modifier: Modifier,
    homeScreenViewModel: HomeScreenViewModel = hiltViewModel(),
    playbackControllerViewModel: PlaybackControllerViewModel = hiltViewModel(),
    addSongsViewModel: AddSongsViewModel = hiltViewModel()
) {
    val songs by homeScreenViewModel.songs.collectAsState()
    val uiState by playbackControllerViewModel.uiState.collectAsState()
    val addState by addSongsViewModel.state.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf("") }
    var isPlaylistMode by remember { mutableStateOf(false) }

    var showPlaylistDialog by remember { mutableStateOf(false) }
    var isAddToExistingMode by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var selectedSongIds by remember { mutableStateOf(setOf<Int>()) }

    val playlists by homeScreenViewModel.playlists.collectAsState()
    val playlistState by homeScreenViewModel.state.collectAsState()


    Column(modifier = modifier.padding(8.dp)) {
        LazyColumn(
            modifier = Modifier
                .weight(1f),
//            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(count = songs.size, key = { index -> songs[index].id }) {
                val song = songs[it]
                SongItemUI(
                    song = song,
                    onPlay = { playbackControllerViewModel.playSongs(songs, it) }
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ){
            CustomButton(
                onClick = { showAddDialog = true }
            ){
                Text(
                    text = "Add Songs",
                    color = text,
                    modifier = Modifier.padding(8.dp)
                )
            }

            CustomButton(
                onClick = { showPlaylistDialog = true }
            ){
                Text(
                    text = "Add Playlist",
                    color = text,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

//        AnimatedVisibility(
//            modifier = Modifier.weight(0.15f),
//            visible = uiState.isPlaying
//        ){
            PlaybackControls(
                modifier = Modifier.weight(0.15f),
                uiState = uiState,
                onBack = { playbackControllerViewModel.previous() },
                onPause = { playbackControllerViewModel.pause() },
                onNext = { playbackControllerViewModel.next() },
            )
//        }
    }

    if (showAddDialog) {
        AddSongDialog(
            onDismiss = {
                showAddDialog = false
                addSongsViewModel.resetState()
            },
            url = url,
            onUrlChange = { url = it },
            isPlaylistMode = isPlaylistMode,
            onModeChange = {
                isPlaylistMode = it
                addSongsViewModel.resetState()
            },
            state = addState,
            onSubmit = {
                if (url.isNotBlank()) {
                    if (isPlaylistMode) addSongsViewModel.addPlaylist(url)
                    else addSongsViewModel.addSingleSong(url)
                }
            },
            onSuccess = {
                showAddDialog = false
            }
        )
    }

    if (showPlaylistDialog) {
        PlaylistDialog(
            onDismiss = {
                showPlaylistDialog = false
                homeScreenViewModel.resetState()
                selectedSongIds = emptySet()
            },
            isAddToExistingMode = isAddToExistingMode,
            onModeChange = {
                isAddToExistingMode = it
                homeScreenViewModel.resetState()
            },
            newPlaylistName = newPlaylistName,
            onNewPlaylistNameChange = { newPlaylistName = it },
            playlists = playlists,
            selectedPlaylist = selectedPlaylist,
            onSelectPlaylist = { selectedPlaylist = it },
            allSongs = songs,
            selectedSongIds = selectedSongIds,
            onToggleSong = { song ->
                selectedSongIds = if (selectedSongIds.contains(song.id))
                    selectedSongIds - song.id else selectedSongIds + song.id
            },
            state = playlistState,
            onSubmit = {
                if (isAddToExistingMode) {
                    selectedPlaylist?.let { homeScreenViewModel.addSongsToPlaylist(it, selectedSongIds) }
                } else {
                    homeScreenViewModel.createPlaylistWithSongs(newPlaylistName, selectedSongIds)
                }
            }
        )
    }
}

