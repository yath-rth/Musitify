package com.yatharth.musitify.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.yatharth.musitify.data.entity.Playlist
import com.yatharth.musitify.data.entity.PlaylistInfo
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.viewmodel.AddSongsViewModel
import com.yatharth.musitify.data.viewmodel.HomeScreenViewModel
import com.yatharth.musitify.data.viewmodel.PlaybackControllerViewModel
import com.yatharth.musitify.ui.elements.AddSongDialog
import com.yatharth.musitify.ui.elements.CustomAlertBox
import com.yatharth.musitify.ui.elements.CustomCircularButton
import com.yatharth.musitify.ui.elements.PlaybackControls
import com.yatharth.musitify.ui.elements.PlaylistDialog
import com.yatharth.musitify.ui.elements.ShadowBox
import com.yatharth.musitify.ui.elements.SortIcon
import com.yatharth.musitify.ui.elements.display.MusicCarousel
import com.yatharth.musitify.ui.elements.display.PlaylistCarousel
import com.yatharth.musitify.ui.theme.adaptiveTypography
import com.yatharth.musitify.ui.theme.background
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.text
import com.yatharth.musitify.util.Formatter
import com.yatharth.musitify.util.SongMedia.playCountOf
import java.io.File

@Composable
fun HomeScreen(
    modifier: Modifier,
    homeScreenViewModel: HomeScreenViewModel = hiltViewModel(),
    playbackControllerViewModel: PlaybackControllerViewModel = hiltViewModel(),
    addSongsViewModel: AddSongsViewModel = hiltViewModel(),
    onOpenStats: () -> Unit = {}
) {
    val TAG = "HomeScreen"

    val allSongs by homeScreenViewModel.sortedSongs.collectAsState()
    val filteredSongs by homeScreenViewModel.searchResults.collectAsState()
    val uiState by playbackControllerViewModel.uiState.collectAsState()
    val addState by addSongsViewModel.state.collectAsState()
    val songSortMode by homeScreenViewModel.songSortMode.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf("") }
    var isPlaylistMode by remember { mutableStateOf(false) }
    var isListMode by remember { mutableStateOf(false) }

    var showPlaylistDialog by remember { mutableStateOf(false) }
    var isAddToExistingMode by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    val songFilterQuery by homeScreenViewModel.searchQuery.collectAsState()
    var selectedSongIds by remember { mutableStateOf(setOf<Song>()) }

    val playlists by homeScreenViewModel.sortedPlaylists.collectAsState()
    val playlistState by homeScreenViewModel.state.collectAsState()
    val songsInPlaylist by homeScreenViewModel.songsInPlaylist.collectAsState()
    val selectedPlaylistId by homeScreenViewModel.playlistId.collectAsState()
    val playlistSortMode by homeScreenViewModel.playlistSortMode.collectAsState()

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
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
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
                    createPlaylist = { showPlaylistDialog = true },
                    onStats = onOpenStats
                )
            }
        },
        floatingActionButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomCircularButton(
                    modifier = Modifier.size(56.dp),
                    onClick = { homeScreenViewModel.syncLibrary() },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowUpward,
                        contentDescription = "Sync your library",
                        modifier = Modifier.size(24.dp),
                        tint = cardBorder
                    )

                    Icon(
                        imageVector = Icons.Outlined.ArrowDownward,
                        contentDescription = "Fetch library",
                        modifier = Modifier.size(24.dp),
                        tint = cardBorder
                    )
                }

//                CustomCircularButton(
//                    modifier = Modifier.size(56.dp),
//                    onClick = { homeScreenViewModel.fetchLibrary() },
//                ) {
//                    Icon(
//                        imageVector = Icons.Outlined.ArrowDownward,
//                        contentDescription = "Fetch library",
//                        modifier = Modifier.size(24.dp),
//                        tint = cardBorder
//                    )
//                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .navigationBarsPadding()
                .padding(innerPadding)
                .padding(8.dp)
        ) {

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedPlaylist?.playlist?.name?.uppercase() ?: "PLAYLISTS",
                        color = text,
                        style = adaptiveTypography().labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    if (selectedPlaylist == null) {
                        SortIcon(
                            mode = playlistSortMode,
                            onClick = {
                                homeScreenViewModel.onPlaylistSortModeToggle()
                            }
                        )
                    }
                }

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
                            style = adaptiveTypography().labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        SortIcon(
                            mode = songSortMode,
                            onClick = {
                                homeScreenViewModel.onSongSortModeToggle()
                            }
                        )

                        IconButton(onClick = { isListMode = !isListMode }) {
                            Icon(
                                imageVector = if (isListMode) Icons.Outlined.ViewCarousel else Icons.Outlined.List,
                                contentDescription = if (isListMode) "Switch to carousel view" else "Switch to list view",
                                tint = cardBorder
                            )
                        }

                    }
                }

                AnimatedVisibility(visible = songsInPlaylist.isEmpty() && !isListMode) {
                    MusicCarousel(
                        songs = allSongs,
//                        size = 140,
//                        spacing = -5,
//                        threshold = 0.5f,
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

                AnimatedVisibility(visible = songsInPlaylist.isEmpty() && isListMode) {
                    ShadowBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        )
                        {
                            LibrarySearchBar(
                                query = songFilterQuery,
                                onQueryChange = { homeScreenViewModel.onSearchQueryChange(it) },
                                placeholder = "search your library"
                            )
                            LazyColumn(
                                Modifier.padding(vertical = 12.dp)
                            ) {
                                val listedSongs =
                                    if (songFilterQuery.isBlank()) allSongs else filteredSongs
                                items(listedSongs) { song ->
                                    SongListRow(
                                        song = song,
                                        isPlaying = song.name == uiState.currentTitle,
                                        onClick = {
                                            playbackControllerViewModel.playSongs(
                                                listedSongs,
                                                listedSongs.indexOf(song),
                                                null
                                            )
                                        },
                                        onLongClick = {
                                            songToDelete = song.id to song.name
                                            showSongDeleteConfirm.value = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                uiState.currentTitle?.let {
                    PlaybackScreen(
                        modifier = Modifier.fillMaxWidth(),
                        song = allSongs[allSongs.indexOfFirst { song -> song.name == it }
                            .coerceAtLeast(0)],
                        playing = uiState.isPlaying
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
                    selectedSongIds = if (selectedSongIds.contains(song))
                        selectedSongIds - song else selectedSongIds + song
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

/**
 * Dense search entry point for the list-mode browse view. Sits above the
 * row list so finding something in a large library doesn't rely on scroll
 * position — this is the highest-leverage fix for library exploration.
 */
@Composable
private fun LibrarySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = placeholder,
                style = adaptiveTypography().bodyMedium,
                color = text.copy(alpha = 0.5f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = cardBorder
            )
        },
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

/**
 * A single row in the dense All Songs list. Replaces the carousel as the
 * default way to move through the full library — ~40dp art instead of a
 * full-width card means a screen shows ~8 songs instead of ~1.5.
 *
 * NOTE: song.duration / song.playCount are guessed field names — adjust
 * to match your actual Song entity.
 */
@Composable
private fun SongListRow(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        AsyncImage(
            model = File(song.artPath),
            contentDescription = song.name,
            modifier = Modifier
                .size(34.dp)
                .aspectRatio(1f),
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.name,
                color = if (isPlaying) cardBorder else text,
                style = adaptiveTypography().bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${Formatter.formatTime(song.length)} · ${playCountOf(song)} plays",
                color = text.copy(alpha = 0.6f),
                style = adaptiveTypography().labelSmall
            )
        }
    }
}

/**
 * A single row in the dense playlist list. Mirrors SongListRow so the
 * two browse screens read as one consistent system rather than two
 * different densities living side by side.
 *
 * NOTE: playlist.playlist.songCount is a guessed field — adjust to match
 * your actual PlaylistInfo / Playlist entity.
 */
@Composable
private fun PlaylistListRow(
    playlist: PlaylistInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {

        AsyncImage(
            model = File(playlist.playlist.artPath),
            contentDescription = playlist.playlist.name,
            modifier = Modifier
                .size(34.dp)
                .aspectRatio(1f),
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.playlist.name,
                color = text,
                style = adaptiveTypography().bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${playlist.numOfSongs} songs",
                color = text.copy(alpha = 0.6f),
                style = adaptiveTypography().labelSmall
            )
        }
    }
}