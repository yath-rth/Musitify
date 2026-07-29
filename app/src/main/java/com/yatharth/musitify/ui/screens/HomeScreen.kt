package com.yatharth.musitify.ui.screens

import android.R.attr.fontWeight
import android.R.attr.name
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.common.collect.Multimaps.index
import com.yatharth.musitify.data.entity.Playlist
import com.yatharth.musitify.data.entity.PlaylistInfo
import com.yatharth.musitify.data.viewmodel.AddSongsViewModel
import com.yatharth.musitify.data.viewmodel.HomeScreenViewModel
import com.yatharth.musitify.data.viewmodel.PlaybackControllerViewModel
import com.yatharth.musitify.ui.elements.AddSongDialog
import com.yatharth.musitify.ui.elements.CustomAlertBox
import com.yatharth.musitify.ui.elements.CustomButton
import com.yatharth.musitify.ui.elements.CustomSwitch
import com.yatharth.musitify.ui.elements.display.MusicCarousel
import com.yatharth.musitify.ui.elements.PlaybackControls
import com.yatharth.musitify.ui.elements.PlaylistDialog
import com.yatharth.musitify.ui.elements.SongItemUI
import com.yatharth.musitify.ui.elements.SortIcon
import com.yatharth.musitify.ui.elements.display.PlaylistCarousel
import com.yatharth.musitify.ui.theme.adaptiveTypography
import com.yatharth.musitify.ui.theme.background
import com.yatharth.musitify.ui.theme.text

@Composable
fun HomeScreen(
    modifier: Modifier,
    homeScreenViewModel: HomeScreenViewModel = hiltViewModel(),
    playbackControllerViewModel: PlaybackControllerViewModel = hiltViewModel(),
    addSongsViewModel: AddSongsViewModel = hiltViewModel()
) {
    val TAG = "HomeScreen"

    val allSongs by homeScreenViewModel.sortedSongs.collectAsState()
    val filteredSongs by homeScreenViewModel.searchResults.collectAsState()
    val uiState by playbackControllerViewModel.uiState.collectAsState()
    val addState by addSongsViewModel.state.collectAsState()
    val sortMode by homeScreenViewModel.sortMode.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf("") }
    var isPlaylistMode by remember { mutableStateOf(false) }
    var isListMode by remember { mutableStateOf(false) }

    var showPlaylistDialog by remember { mutableStateOf(false) }
    var isAddToExistingMode by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    val songFilterQuery by homeScreenViewModel.searchQuery.collectAsState()
    var selectedSongIds by remember { mutableStateOf(setOf<Int>()) }

    val playlists by homeScreenViewModel.playlists.collectAsState()
    val playlistState by homeScreenViewModel.state.collectAsState()
    val songsInPlaylist by homeScreenViewModel.songsInPlaylist.collectAsState()
    val selectedPlaylistId by homeScreenViewModel.playlistId.collectAsState()

    var songToDelete by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    val showSongDeleteConfirm = remember { mutableStateOf(false) }
    val showPlaylistDeleteConfirm = remember { mutableStateOf(false) }

    var selectedPlaylist: PlaylistInfo? = remember(selectedPlaylistId) {
        playlists.firstOrNull { it.playlist.id == selectedPlaylistId }
    }

    BackHandler(
        enabled = songsInPlaylist.isNotEmpty()
    ) {
        homeScreenViewModel.clearSelectedPlaylist()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        containerColor = background,
        bottomBar = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                PlaybackControls(
                    modifier = Modifier.fillMaxWidth(),
                    uiState = uiState,
                    onBack = { playbackControllerViewModel.previous() },
                    onPause = { playbackControllerViewModel.pause() },
                    onNext = { playbackControllerViewModel.next() },
                    onRepeatToggle = { playbackControllerViewModel.toggleRepeatMode() },
                    addSong = { showAddDialog = true },
                    createPlaylist = { showPlaylistDialog = true }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .padding(innerPadding)
                .padding(8.dp)
        ) {

            item {
                Text(
                    text = selectedPlaylist?.playlist?.name?.uppercase() ?: "PLAYLISTS",
                    color = text,
                    style = adaptiveTypography().bodyLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                AnimatedVisibility(visible = songsInPlaylist.isEmpty()) {
                    PlaylistCarousel(
                        playlists = playlists,
                        currentPlayingIndex = selectedPlaylistId?.coerceAtLeast(0) ?: 0,
                        onPlaylistSelected = { index -> homeScreenViewModel.loadPlaylist(index) },
                        onLongClick = { playlist ->
                            playlistToDelete = playlist
                            showPlaylistDeleteConfirm.value = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }

                selectedPlaylistId?.let { playlistId ->
                    AnimatedVisibility(visible = songsInPlaylist.isNotEmpty()) {
                        MusicCarousel(
                            songs = songsInPlaylist,
                            currentPlayingIndex = songsInPlaylist.indexOfFirst { it.name == uiState.currentTitle }
                                .coerceAtLeast(0),
                            onTrackSelected = { index ->
                                playbackControllerViewModel.playSongs(
                                    songsInPlaylist,
                                    index,
                                    playlistId
                                )
                            },
                            onLongSelect = { song ->
                                songToDelete = song.id to song.name
                                showSongDeleteConfirm.value = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                AnimatedVisibility(visible = songsInPlaylist.isEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ALL SONGS",
                            color = text,
                            style = adaptiveTypography().bodyLarge,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        SortIcon(
                            mode = sortMode,
                            onClick = {
                                homeScreenViewModel.onSortModeToggle()
                            }
                        )
                    }
                }

                AnimatedVisibility(visible = songsInPlaylist.isEmpty()) {
                    MusicCarousel(
                        songs = allSongs,
                        size = 140,
                        spacing = -5,
                        threshold = 0.5f,
                        scroll = false,
                        currentPlayingIndex = allSongs.indexOfFirst { it.name == uiState.currentTitle }
                            .coerceAtLeast(0),
                        onTrackSelected = { index ->
                            playbackControllerViewModel.playSongs(
                                allSongs,
                                index,
                                null
                            )
                        },
                        onLongSelect = { song ->
                            songToDelete = song.id to song.name
                            showSongDeleteConfirm.value = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }
            }
        }

        if (showAddDialog) {
            AddSongDialog(
                onDismiss = {
                    showAddDialog = false
                    addSongsViewModel.resetState()
                    url = ""
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
                    addSongsViewModel.resetState()
                }
            )
        }

        if (showSongDeleteConfirm.value) {
            songToDelete?.let { (index, name) ->
                CustomAlertBox(
                    showClearConfirm = showSongDeleteConfirm,
                    title = "Delete Song",
                    body = "Are you sure you want to delete \"$name\"?",
                    yes = "Delete",
                    no = "Cancel",
                    onConfirm = {
                        selectedPlaylistId?.let { playlistId ->
                            homeScreenViewModel.deleteSongFromPlaylist(
                                index = index,
                                playlistId = playlistId
                            )
                        } ?: homeScreenViewModel.deleteSong(index)
                        songToDelete = null
                    },
                    onDismiss = { songToDelete = null }
                )
            }
        }

        if (showPlaylistDeleteConfirm.value) {
            playlistToDelete?.let { playlist ->
                CustomAlertBox(
                    showClearConfirm = showPlaylistDeleteConfirm,
                    title = "Delete Playlist",
                    body = "Are you sure you want to delete \"${playlist.name}\"?",
                    yes = "Delete",
                    no = "Cancel",
                    onConfirm = {
                        homeScreenViewModel.deletePlaylist(playlist.id)
                        playlistToDelete = null
                    },
                    onDismiss = { playlistToDelete = null }
                )
            }
        }

        if (showPlaylistDialog) {
            PlaylistDialog(
                onDismiss = {
                    showPlaylistDialog = false
                    homeScreenViewModel.resetState()
                    selectedSongIds = emptySet()
                    newPlaylistName = ""
                },
                isAddToExistingMode = isAddToExistingMode,
                onModeChange = {
                    isAddToExistingMode = it
                    homeScreenViewModel.resetState()
                },
                newPlaylistName = newPlaylistName,
                onNewPlaylistNameChange = { newPlaylistName = it },
                songQuery = songFilterQuery,
                onSongFilter = { homeScreenViewModel.onSearchQueryChange(it) },
                playlists = playlists,
                selectedPlaylist = selectedPlaylist,
                onSelectPlaylist = { selectedPlaylist = it },
                allSongs = filteredSongs,
                selectedSongIds = selectedSongIds,
                onToggleSong = { song ->
                    selectedSongIds = if (selectedSongIds.contains(song.id))
                        selectedSongIds - song.id else selectedSongIds + song.id
                },
                state = playlistState,
                onSubmit = {
                    if (isAddToExistingMode) {
                        selectedPlaylist?.let {
                            homeScreenViewModel.addSongsToPlaylist(
                                it.playlist,
                                selectedSongIds
                            )
                        }
                    } else {
                        homeScreenViewModel.createPlaylistWithSongs(
                            newPlaylistName,
                            selectedSongIds
                        )
                    }
                }
            )
        }
    }
}
