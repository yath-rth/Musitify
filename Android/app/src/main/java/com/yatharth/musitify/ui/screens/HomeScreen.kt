package com.yatharth.musitify.ui.screens

import androidx.activity.compose.BackHandler
import androidx.collection.CircularArray
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Reorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.yatharth.musitify.data.viewmodel.AddSongsUiState
import com.yatharth.musitify.data.viewmodel.AddSongsViewModel
import com.yatharth.musitify.data.viewmodel.HomeScreenViewModel
import com.yatharth.musitify.data.viewmodel.PlaybackControllerViewModel
import com.yatharth.musitify.service.DownloadJob
import com.yatharth.musitify.service.DownloadStatus
import com.yatharth.musitify.service.DownloadType
import com.yatharth.musitify.ui.elements.AddSongDialog
import com.yatharth.musitify.ui.elements.AddToPlaylistDialog
import com.yatharth.musitify.ui.elements.CustomAlertBox
import com.yatharth.musitify.ui.elements.CustomCircularButton
import com.yatharth.musitify.ui.elements.PlaybackControls
import com.yatharth.musitify.ui.elements.PlaylistContextMenuDialog
import com.yatharth.musitify.ui.elements.PlaylistDialog
import com.yatharth.musitify.ui.elements.RenamePlaylistDialog
import com.yatharth.musitify.ui.elements.RenameSongDialog
import com.yatharth.musitify.ui.elements.ReorderableSongList
import com.yatharth.musitify.ui.elements.ShadowBox
import com.yatharth.musitify.ui.elements.SleepTimerDialog
import com.yatharth.musitify.ui.elements.SongContextMenuDialog
import com.yatharth.musitify.ui.elements.SortIcon
import com.yatharth.musitify.ui.elements.display.MusicCarousel
import com.yatharth.musitify.ui.elements.display.PlaylistCarousel
import com.yatharth.musitify.ui.theme.accentColor2
import com.yatharth.musitify.ui.theme.adaptiveTypography
import com.yatharth.musitify.ui.theme.background
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.text
import com.yatharth.musitify.ui.theme.textSecondary
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
    val downloadJobs by addSongsViewModel.jobs.collectAsState()
    val songSortMode by homeScreenViewModel.songSortMode.collectAsState()
    val sleepTimerState by playbackControllerViewModel.sleepTimerState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var url by remember { mutableStateOf("") }
    var isPlaylistMode by remember { mutableStateOf(false) }
    var isListMode by remember { mutableStateOf(false) }
    var isReorderMode by remember { mutableStateOf(false) }

    var showPlaylistDialog by remember { mutableStateOf(false) }
    var isAddToExistingMode by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    val songFilterQuery by homeScreenViewModel.searchQuery.collectAsState()
    var selectedSongIds by remember { mutableStateOf(setOf<Song>()) }
    var selectedPlaylistForDialog by remember { mutableStateOf<PlaylistInfo?>(null) }

    val playlists by homeScreenViewModel.sortedPlaylists.collectAsState()
    val playlistState by homeScreenViewModel.state.collectAsState()
    val songsInPlaylist by homeScreenViewModel.songsInPlaylist.collectAsState()
    val selectedPlaylistId by homeScreenViewModel.playlistId.collectAsState()
    val playlistSortMode by homeScreenViewModel.playlistSortMode.collectAsState()

    var songToDelete by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    val showSongDeleteConfirm = remember { mutableStateOf(false) }
    val showPlaylistDeleteConfirm = remember { mutableStateOf(false) }

    // Song context menu states
    var songForContextMenu by remember { mutableStateOf<Song?>(null) }
    var showSongContextMenu by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showRenameSongDialog by remember { mutableStateOf(false) }
    var newSongName by remember { mutableStateOf("") }
    var selectedPlaylistForAddSong by remember { mutableStateOf<PlaylistInfo?>(null) }
    var isAddToExistingPlaylistMode by remember { mutableStateOf(false) }
    var newPlaylistNameForSong by remember { mutableStateOf("") }

    // Playlist context menu states
    var playlistForContextMenu by remember { mutableStateOf<Playlist?>(null) }
    var showPlaylistContextMenu by remember { mutableStateOf(false) }
    var showRenamePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistNameForRename by remember { mutableStateOf("") }

    val selectedPlaylist: PlaylistInfo? = remember(selectedPlaylistId) {
        playlists.firstOrNull { it.playlist.id == selectedPlaylistId }
    }

    // Once a URL is queued, close the dialog; the queue card shows progress from here.
    LaunchedEffect(addState) {
        if (addState is AddSongsUiState.Queued) {
            showAddDialog = false
            url = ""
            addSongsViewModel.resetState()
        }
    }

    BackHandler(
        enabled = songsInPlaylist.isNotEmpty()
    ) {
        homeScreenViewModel.clearSelectedPlaylist()
    }

    // Prefer the id; fall back to the title for tracks queued before ids were tracked.
    val currentSong: Song? = allSongs.firstOrNull { it.id == uiState.currentSongId }
        ?: allSongs.firstOrNull { it.name == uiState.currentTitle }
    val hasTrack = uiState.currentTitle != null

    // Declared after the playlist handler so it wins while the player is open.
    BackHandler(enabled = showNowPlaying) { showNowPlaying = false }

    Box(modifier = Modifier.fillMaxSize()) {
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
                CustomCircularButton(
                    modifier = modifier,
                    onClick = {
                        homeScreenViewModel.syncLibrary()
                        homeScreenViewModel.fetchLibrary()
                    },
                ){
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                PlaybackControls(
                    modifier = Modifier.fillMaxWidth(),
                    uiState = uiState,
                    onBack = { playbackControllerViewModel.previous() },
                    onPause = { playbackControllerViewModel.pause() },
                    onNext = { playbackControllerViewModel.next() },
                    onRepeatToggle = { playbackControllerViewModel.toggleRepeatMode() },
                    onShuffleToggle = { playbackControllerViewModel.toggleShuffleMode() },
                    addSong = { showAddDialog = true },
                    createPlaylist = { showPlaylistDialog = true },
                    onStats = onOpenStats,
                    onSleepTimer = { showSleepTimerDialog = true },
                    sleepTimerActive = sleepTimerState.isActive,
                    onOpenNowPlaying = if (hasTrack) ({ showNowPlaying = true }) else null
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .navigationBarsPadding()
                .padding(innerPadding)
                .padding(8.dp)
        ) {

            // ---- Download queue ----
            if (downloadJobs.isNotEmpty()) {
                item(key = "download_queue") {
                    DownloadQueueCard(
                        jobs = downloadJobs,
                        onCancel = { addSongsViewModel.cancel(it) },
                        onRetry = { addSongsViewModel.retry(it) },
                        onClearFinished = { addSongsViewModel.clearFinished() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }
            }

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
                    } else {
                        // Show reorder button when a playlist is selected
                        IconButton(onClick = { isReorderMode = !isReorderMode }) {
                            Icon(
                                imageVector = Icons.Outlined.Reorder,
                                contentDescription = if (isReorderMode) "Exit reorder mode" else "Reorder songs",
                                tint = if (isReorderMode) accentColor2 else cardBorder
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = songsInPlaylist.isEmpty()) {
                    PlaylistCarousel(
                        playlists = playlists,
                        currentPlayingIndex = selectedPlaylistId?.coerceAtLeast(0) ?: 0,
                        onPlaylistSelected = { index -> homeScreenViewModel.loadPlaylist(index) },
                        onLongClick = { playlist ->
                            playlistForContextMenu = playlist
                            showPlaylistContextMenu = true
                        },
                        onCreatePlaylist = { showPlaylistDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }

                selectedPlaylistId?.let { playlistId ->
                    AnimatedVisibility(visible = songsInPlaylist.isNotEmpty() && !isReorderMode) {
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
                                songForContextMenu = song
                                showSongContextMenu = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }

                    AnimatedVisibility(visible = songsInPlaylist.isNotEmpty() && isReorderMode) {
                        Column {
                            Text(
                                text = "USE ARROWS TO REORDER SONGS",
                                color = accentColor2,
                                style = adaptiveTypography().labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            ReorderableSongList(
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
                                onReorder = { fromIndex, toIndex ->
                                    homeScreenViewModel.reorderSongsInPlaylist(fromIndex, toIndex)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                            )
                        }
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
                            songForContextMenu = song
                            showSongContextMenu = true
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
                            val listedSongs =
                                if (songFilterQuery.isBlank()) allSongs else filteredSongs
                            if (listedSongs.isEmpty() && songFilterQuery.isNotBlank()) {
                                NoSearchResults(
                                    query = songFilterQuery,
                                    onClear = { homeScreenViewModel.onSearchQueryChange("") }
                                )
                            }
                            LazyColumn(
                                Modifier.padding(vertical = 12.dp)
                            ) {
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
                                        // Same menu as the carousel; delete is one of its options.
                                        onLongClick = {
                                            songForContextMenu = song
                                            showSongContextMenu = true
                                        }
                                    )
                                }
                            }
                        }
                    }
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
                    selectedPlaylistForDialog = null
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
                selectedPlaylist = selectedPlaylistForDialog,
                onSelectPlaylist = { selectedPlaylistForDialog = it },
                allSongs = filteredSongs,
                selectedSongIds = selectedSongIds,
                onToggleSong = { song ->
                    selectedSongIds = if (selectedSongIds.contains(song))
                        selectedSongIds - song else selectedSongIds + song
                },
                state = playlistState,
                onSubmit = {
                    if (isAddToExistingMode) {
                        selectedPlaylistForDialog?.let {
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

        // Song context menu dialog
        if (showSongContextMenu && songForContextMenu != null) {
            SongContextMenuDialog(
                song = songForContextMenu!!,
                onDismiss = {
                    showSongContextMenu = false
                    songForContextMenu = null
                },
                onAddToPlaylist = {
                    showSongContextMenu = false
                    showAddToPlaylistDialog = true
                },
                onRename = {
                    showSongContextMenu = false
                    newSongName = songForContextMenu!!.name
                    showRenameSongDialog = true
                },
                onDelete = {
                    showSongContextMenu = false
                    songToDelete = songForContextMenu!!.id to songForContextMenu!!.name
                    showSongDeleteConfirm.value = true
                    songForContextMenu = null
                }
            )
        }

        // Add to playlist dialog
        if (showAddToPlaylistDialog && songForContextMenu != null) {
            AddToPlaylistDialog(
                song = songForContextMenu!!,
                onDismiss = {
                    showAddToPlaylistDialog = false
                    selectedPlaylistForAddSong = null
                    newPlaylistNameForSong = ""
                    songForContextMenu = null
                    isAddToExistingPlaylistMode = false
                    homeScreenViewModel.resetState()
                },
                isAddToExistingMode = isAddToExistingPlaylistMode,
                onModeChange = {
                    isAddToExistingPlaylistMode = it
                    homeScreenViewModel.resetState()
                },
                newPlaylistName = newPlaylistNameForSong,
                onNewPlaylistNameChange = { newPlaylistNameForSong = it },
                playlists = playlists,
                selectedPlaylist = selectedPlaylistForAddSong,
                onSelectPlaylist = { selectedPlaylistForAddSong = it },
                state = playlistState,
                onSubmit = {
                    if (isAddToExistingPlaylistMode) {
                        selectedPlaylistForAddSong?.let {
                            homeScreenViewModel.addSongsToPlaylist(
                                it.playlist,
                                setOf(songForContextMenu!!)
                            )
                        }
                    } else {
                        if (newPlaylistNameForSong.isNotBlank()) {
                            homeScreenViewModel.createPlaylistWithSongs(
                                newPlaylistNameForSong,
                                setOf(songForContextMenu!!)
                            )
                        }
                    }
                }
            )
        }

        // Rename song dialog
        if (showRenameSongDialog && songForContextMenu != null) {
            RenameSongDialog(
                song = songForContextMenu!!,
                newName = newSongName,
                onNameChange = { newSongName = it },
                onDismiss = {
                    showRenameSongDialog = false
                    newSongName = ""
                    songForContextMenu = null
                },
                onConfirm = {
                    if (newSongName.isNotBlank() && newSongName != songForContextMenu!!.name) {
                        homeScreenViewModel.renameSong(songForContextMenu!!, newSongName)
                    }
                    showRenameSongDialog = false
                    newSongName = ""
                    songForContextMenu = null
                }
            )
        }

        // Playlist context menu dialog
        if (showPlaylistContextMenu && playlistForContextMenu != null) {
            PlaylistContextMenuDialog(
                playlist = playlistForContextMenu!!,
                onDismiss = {
                    showPlaylistContextMenu = false
                    playlistForContextMenu = null
                },
                onAddSongs = {
                    showPlaylistContextMenu = false
                    showPlaylistDialog = true
                    isAddToExistingMode = true
                    selectedPlaylistForDialog = playlists.firstOrNull { it.playlist.id == playlistForContextMenu!!.id }
                },
                onRename = {
                    showPlaylistContextMenu = false
                    newPlaylistNameForRename = playlistForContextMenu!!.name
                    showRenamePlaylistDialog = true
                },
                onDelete = {
                    showPlaylistContextMenu = false
                    playlistToDelete = playlistForContextMenu
                    showPlaylistDeleteConfirm.value = true
                    playlistForContextMenu = null
                }
            )
        }

        // Rename playlist dialog
        if (showRenamePlaylistDialog && playlistForContextMenu != null) {
            RenamePlaylistDialog(
                playlist = playlistForContextMenu!!,
                newName = newPlaylistNameForRename,
                onNameChange = { newPlaylistNameForRename = it },
                onDismiss = {
                    showRenamePlaylistDialog = false
                    newPlaylistNameForRename = ""
                    playlistForContextMenu = null
                },
                onConfirm = {
                    if (newPlaylistNameForRename.isNotBlank() && newPlaylistNameForRename != playlistForContextMenu!!.name) {
                        homeScreenViewModel.renamePlaylist(playlistForContextMenu!!, newPlaylistNameForRename)
                    }
                    showRenamePlaylistDialog = false
                    newPlaylistNameForRename = ""
                    playlistForContextMenu = null
                }
            )
        }

        // Sleep timer dialog
        if (showSleepTimerDialog) {
            SleepTimerDialog(
                sleepTimerState = sleepTimerState,
                onSetTimer = { duration ->
                    playbackControllerViewModel.startSleepTimer(duration)
                },
                onCancelTimer = {
                    playbackControllerViewModel.cancelSleepTimer()
                },
                onDismiss = {
                    showSleepTimerDialog = false
                }
            )
        }
    }

    AnimatedVisibility(
        visible = showNowPlaying && hasTrack,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        NowPlayingScreen(
            song = currentSong,
            uiState = uiState,
            sleepTimerState = sleepTimerState,
            onClose = { showNowPlaying = false },
            onPlayPause = { playbackControllerViewModel.pause() },
            onPrevious = { playbackControllerViewModel.previous() },
            onNext = { playbackControllerViewModel.next() },
            onSeek = { playbackControllerViewModel.seekTo(it) },
            onRepeatToggle = { playbackControllerViewModel.toggleRepeatMode() },
            onShuffleToggle = { playbackControllerViewModel.toggleShuffleMode() },
            onSleepTimer = { showSleepTimerDialog = true }
        )
    }
    }
}

/**
 * Download queue card. Active jobs are listed first, finished ones after.
 * Shows the first few rows and expands on tap so a long queue doesn't
 * push the whole library off screen.
 */
@Composable
private fun DownloadQueueCard(
    jobs: List<DownloadJob>,
    onCancel: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onClearFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val ordered = remember(jobs) { jobs.sortedBy { !it.isActive } } // stable: active first
    val activeCount = jobs.count { it.isActive }
    val hasFinished = jobs.any { !it.isActive }
    val visible = if (expanded) ordered else ordered.take(COLLAPSED_ROWS)
    val hiddenCount = ordered.size - visible.size

    ShadowBox(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (activeCount > 0) "DOWNLOADS · $activeCount ACTIVE" else "DOWNLOADS",
                    color = text,
                    style = adaptiveTypography().labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
                if (hasFinished) {
                    TextButton(onClick = onClearFinished) {
                        Text(
                            text = "Clear",
                            color = cardBorder,
                            style = adaptiveTypography().labelMedium
                        )
                    }
                }
            }

            visible.forEach { job ->
                DownloadJobRow(
                    job = job,
                    onCancel = { onCancel(job.id) },
                    onRetry = { onRetry(job.id) }
                )
            }

            if (hiddenCount > 0 || expanded && ordered.size > COLLAPSED_ROWS) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = if (expanded) "Show less" else "Show $hiddenCount more",
                        color = cardBorder,
                        style = adaptiveTypography().labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadJobRow(
    job: DownloadJob,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusText = when (job.status) {
        DownloadStatus.QUEUED -> "Waiting in queue"
        DownloadStatus.RUNNING ->
            if (job.type == DownloadType.PLAYLIST && job.total > 0)
                "Downloading ${job.completed} of ${job.total}"
            else "Downloading…"
        DownloadStatus.COMPLETED ->
            if (job.type == DownloadType.PLAYLIST && job.total > 1) "Done · ${job.total} songs"
            else "Done"
        DownloadStatus.FAILED -> job.error?.let { "Failed · $it" } ?: "Failed"
        DownloadStatus.CANCELLED -> "Cancelled"
    }

    val statusColor =
        if (job.status == DownloadStatus.FAILED) text else textSecondary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = (if (job.type == DownloadType.PLAYLIST) "Playlist · " else "Song · ") +
                        displayUrl(job.url),
                color = text,
                style = adaptiveTypography().bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = statusText,
                color = statusColor,
                style = adaptiveTypography().labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (job.status == DownloadStatus.RUNNING) {
                Spacer(modifier = Modifier.height(6.dp))
                if (job.total > 0) {
                    LinearProgressIndicator(
                        progress = { (job.completed.toFloat() / job.total).coerceIn(0f, 1f) },
                        color = cardBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                    )
                } else {
                    LinearProgressIndicator(
                        color = cardBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                    )
                }
            }
        }

        when {
            job.isActive -> IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Cancel download",
                    tint = cardBorder
                )
            }
            job.status == DownloadStatus.FAILED || job.status == DownloadStatus.CANCELLED ->
                IconButton(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Retry download",
                        tint = cardBorder
                    )
                }
        }
    }
}

private const val COLLAPSED_ROWS = 3

/** "https://www.youtube.com/watch?v=abc" -> "youtube.com/watch?v=abc" */
private fun displayUrl(url: String): String =
    url.removePrefix("https://").removePrefix("http://").removePrefix("www.")

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
                color = textSecondary
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

/** Shown in place of the list when a search matches nothing. */
@Composable
private fun NoSearchResults(query: String, onClear: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "No songs match \"$query\"",
            color = text,
            style = adaptiveTypography().bodyMedium
        )
        Text(
            text = "Check the spelling, or search by artist.",
            color = textSecondary,
            style = adaptiveTypography().labelSmall
        )
        TextButton(onClick = onClear) {
            Text(
                text = "CLEAR SEARCH",
                color = text,
                style = adaptiveTypography().labelMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

/**
 * A single row in the dense All Songs list. Replaces the carousel as the
 * default way to move through the full library — ~40dp art instead of a
 * full-width card means a screen shows ~8 songs instead of ~1.5.
 *
 * NOTE: song.duration / song.playCount are guessed field names — adjust
 * to match your actual Song entity.
 */
@OptIn(ExperimentalFoundationApi::class)
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
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
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
                text = "${Formatter.formatTime(song.length)} · ${Formatter.count(playCountOf(song), "play")}",
                color = textSecondary,
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
                text = Formatter.count(playlist.numOfSongs, "song"),
                color = textSecondary,
                style = adaptiveTypography().labelSmall
            )
        }
    }
}