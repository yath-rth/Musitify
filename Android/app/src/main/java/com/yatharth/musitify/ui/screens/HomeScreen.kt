package com.yatharth.musitify.ui.screens

import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.QueueMusic
import com.yatharth.musitify.ui.elements.EmptyState
import com.yatharth.musitify.ui.elements.LoadingState
import com.yatharth.musitify.util.SongDisplay
import androidx.activity.compose.BackHandler
import androidx.collection.CircularArray
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.outlined.Download
import com.yatharth.musitify.ui.theme.itemsBg
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import kotlinx.coroutines.channels.Channel
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import com.yatharth.musitify.ui.theme.primaryAction
import com.yatharth.musitify.ui.elements.CustomButton
import com.yatharth.musitify.ui.elements.OutlinedInput
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.outlined.Add
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.graphics.Color
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
import com.yatharth.musitify.data.repository.YouTubeResult
import com.yatharth.musitify.data.viewmodel.AddSongsUiState
import com.yatharth.musitify.data.viewmodel.YouTubeSearchState
import com.yatharth.musitify.data.viewmodel.AddSongsViewModel
import com.yatharth.musitify.data.viewmodel.HomeScreenViewModel
import com.yatharth.musitify.data.viewmodel.PlaybackControllerViewModel
import com.yatharth.musitify.data.viewmodel.PlaylistDialogUiState
import com.yatharth.musitify.service.DownloadJob
import com.yatharth.musitify.service.DownloadStatus
import com.yatharth.musitify.service.DownloadType
import com.yatharth.musitify.ui.elements.AddSongDialog
import com.yatharth.musitify.ui.elements.AddToPlaylistDialog
import com.yatharth.musitify.ui.elements.CustomAlertBox
import com.yatharth.musitify.ui.elements.CustomCircularButton
import com.yatharth.musitify.ui.elements.PlaybackControls
import com.yatharth.musitify.ui.elements.PlaylistContextMenuDialog
import com.yatharth.musitify.ui.elements.AddSongsToPlaylistDialog
import com.yatharth.musitify.ui.elements.CreatePlaylistDialog
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
import com.yatharth.musitify.ui.theme.dotGridBackground
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.text
import com.yatharth.musitify.ui.theme.textSecondary
import com.yatharth.musitify.util.Formatter
import com.yatharth.musitify.util.LinkDetector
import com.yatharth.musitify.util.SongMedia.playCountOf
import java.io.File

@Composable
fun HomeScreen(
    modifier: Modifier,
    homeScreenViewModel: HomeScreenViewModel = hiltViewModel(),
    playbackControllerViewModel: PlaybackControllerViewModel = hiltViewModel(),
    addSongsViewModel: AddSongsViewModel = hiltViewModel(),
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
    var tab by rememberSaveable { mutableStateOf(HomeTab.LIBRARY) }
    var url by remember { mutableStateOf("") }
    var isListMode by remember { mutableStateOf(false) }
    var isReorderMode by remember { mutableStateOf(false) }

    var showPlaylistDialog by remember { mutableStateOf(false) }
    var isAddToExistingMode by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    val songFilterQuery by homeScreenViewModel.searchQuery.collectAsState()
    val youTubeSearch by homeScreenViewModel.youTubeSearch.collectAsState()
    val previewingId by homeScreenViewModel.previewingId.collectAsState()
    val previewError by homeScreenViewModel.previewError.collectAsState()
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

    val selectedPlaylist: PlaylistInfo? =
        playlists.firstOrNull { it.playlist.id == selectedPlaylistId }

    // Finished downloads leave the card and are announced once at the bottom,
    // so the card only takes space while there's something to watch or retry.
    val snackbarHostState = remember { SnackbarHostState() }
    val announcements = remember { Channel<String>(Channel.UNLIMITED) }
    LaunchedEffect(downloadJobs) {
        downloadJobs.filter { it.status == DownloadStatus.COMPLETED }.forEach { job ->
            announcements.trySend(completedMessage(job))
            addSongsViewModel.dismiss(job.id)
        }
    }
    LaunchedEffect(Unit) {
        for (message in announcements) snackbarHostState.showSnackbar(message)
    }
    LaunchedEffect(previewError) {
        previewError?.let {
            announcements.trySend(it)
            homeScreenViewModel.clearPreviewError()
        }
    }
    // A preview shouldn't keep playing once the search that produced it is gone.
    LaunchedEffect(songFilterQuery, tab) {
        if (songFilterQuery.isBlank() || tab != HomeTab.LIBRARY) homeScreenViewModel.stopPreview()
    }
    val visibleJobs = downloadJobs.filter { it.status != DownloadStatus.COMPLETED }

    // Once a URL is queued, close the dialog; the queue card shows progress from here.
    LaunchedEffect(addState) {
        if (addState is AddSongsUiState.Queued) {
            showAddDialog = false
            url = ""
            addSongsViewModel.resetState()
        }
    }

    BackHandler(
        enabled = selectedPlaylistId != null
    ) {
        isReorderMode = false
        homeScreenViewModel.clearSelectedPlaylist()
    }

    // Prefer the id; fall back to the title for tracks queued before ids were tracked.
    val currentSong: Song? = allSongs.firstOrNull { it.id == uiState.currentSongId }
        ?: allSongs.firstOrNull { it.name == uiState.currentTitle }
    val hasTrack = uiState.currentTitle != null

    // Back from Stats returns to the library.
    BackHandler(enabled = tab == HomeTab.STATS) { tab = HomeTab.LIBRARY }

    // Declared after the other handlers so it wins while the player is open.
    BackHandler(enabled = showNowPlaying) { showNowPlaying = false }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .dotGridBackground(),
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data -> InfoToast(data.visuals.message) }
        },
        topBar = {
            Column {
                AppHeader(
                    tab = tab,
                    onTabSelected = { tab = it },
                    onSync = {
                        homeScreenViewModel.syncLibrary()
                        homeScreenViewModel.fetchLibrary()
                    },
                    onAddSongs = { showAddDialog = true }
                )
                // Pinned in the top bar so search is reachable without scrolling.
                if (tab == HomeTab.LIBRARY && selectedPlaylist == null) {
                    LibrarySearchBar(
                        query = songFilterQuery,
                        onQueryChange = { homeScreenViewModel.onSearchQueryChange(it) },
                        placeholder = "search your library or YouTube",
                        modifier = Modifier
                            .fillMaxWidth()
                            .dotGridBackground()
                    )
                }
            }
        },
        bottomBar = {
            PlaybackControls(
                // Solid backing so scrolled content doesn't show through
                // around the bar or behind the system navigation bar.
                modifier = Modifier
                    .fillMaxWidth()
                    .dotGridBackground()
                    .navigationBarsPadding(),
                uiState = uiState,
                title = currentSong?.let { SongDisplay.title(it) },
                onBack = { playbackControllerViewModel.previous() },
                onPause = { playbackControllerViewModel.pause() },
                onNext = { playbackControllerViewModel.next() },
                onRepeatToggle = { playbackControllerViewModel.toggleRepeatMode() },
                onShuffleToggle = { playbackControllerViewModel.toggleShuffleMode() },
                onOpenNowPlaying = if (hasTrack) ({ showNowPlaying = true }) else null
            )
        }
    ) { innerPadding ->
        val listedSongs = if (songFilterQuery.isBlank()) allSongs else filteredSongs

        // contentPadding (not padding) so cards scroll under the player bar
        // instead of being clipped above it.
        if (tab == HomeTab.STATS) {
            StatsContent(contentPadding = innerPadding)
        } else LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 8.dp
            )
        ) {

            // ---- Download queue ----
            if (visibleJobs.isNotEmpty()) {
                item(key = "download_queue") {
                    DownloadQueueCard(
                        jobs = visibleJobs,
                        onCancel = { addSongsViewModel.cancel(it) },
                        onRetry = { addSongsViewModel.retry(it) },
                        onClearFinished = { addSongsViewModel.clearFinished() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }
            }

            if (selectedPlaylist == null && songFilterQuery.isBlank()) {
                item(key = "playlists_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PLAYLISTS",
                            color = text,
                            style = adaptiveTypography().labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        SortIcon(
                            mode = playlistSortMode,
                            onClick = { homeScreenViewModel.onPlaylistSortModeToggle() }
                        )
                        // Creating a playlist is an action, not a playlist, so it
                        // lives here instead of as a full-size card in the carousel.
                        CustomButton(onClick = {
                            isAddToExistingMode = false
                            showPlaylistDialog = true
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "New playlist",
                                tint = cardBorder,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }
                item(key = "playlists_carousel") {
                    Column {
                        if (playlists.isEmpty()) {
                            EmptyHint(
                                title = "No playlists yet",
                                body = "Group songs you like to play together.",
                                action = "NEW PLAYLIST",
                                onAction = {
                                    isAddToExistingMode = false
                                    showPlaylistDialog = true
                                }
                            )
                        } else {
                            PlaylistCarousel(
                                playlists = playlists,
                                currentPlayingIndex = 0,
                                onPlaylistSelected = { id -> homeScreenViewModel.loadPlaylist(id) },
                                onLongClick = { playlist ->
                                    playlistForContextMenu = playlist
                                    showPlaylistContextMenu = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            } else if (selectedPlaylist != null) {
                val playlistId = selectedPlaylist.playlist.id
                item(key = "playlist_header") {
                    PlaylistHeader(
                        info = selectedPlaylist,
                        isReorderMode = isReorderMode,
                        canPlay = songsInPlaylist.isNotEmpty(),
                        onBack = {
                            isReorderMode = false
                            homeScreenViewModel.clearSelectedPlaylist()
                        },
                        onToggleReorder = { isReorderMode = !isReorderMode },
                        onPlay = {
                            playbackControllerViewModel.playSongs(songsInPlaylist, 0, playlistId)
                        },
                        onShuffle = {
                            playbackControllerViewModel.playShuffled(songsInPlaylist, playlistId)
                        }
                    )
                }
                item(key = "playlist_songs") {
                    Column {
                        when {
                            selectedPlaylist.numOfSongs == 0 -> EmptyHint(
                                title = "This playlist is empty",
                                body = "Add songs from your library.",
                                action = "ADD SONGS",
                                onAction = {
                                    isAddToExistingMode = true
                                    selectedPlaylistForDialog = selectedPlaylist
                                    showPlaylistDialog = true
                                }
                            )

                            isReorderMode -> {
                                Text(
                                    text = "Drag the handles to reorder",
                                    color = textSecondary,
                                    style = adaptiveTypography().bodySmall,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                ReorderableSongList(
                                    songs = songsInPlaylist,
                                    currentPlayingIndex = songsInPlaylist.indexOfFirst { it.id == currentSong?.id },
                                    onTrackSelected = { index ->
                                        playbackControllerViewModel.playSongs(songsInPlaylist, index, playlistId)
                                    },
                                    onReorder = { fromIndex, toIndex ->
                                        homeScreenViewModel.reorderSongsInPlaylist(fromIndex, toIndex)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp)
                                )
                            }

                            else -> MusicCarousel(
                                songs = songsInPlaylist,
                                currentPlayingIndex = songsInPlaylist.indexOfFirst { it.id == currentSong?.id }
                                    .coerceAtLeast(0),
                                onTrackSelected = { index ->
                                    playbackControllerViewModel.playSongs(songsInPlaylist, index, playlistId)
                                },
                                onLongSelect = { song ->
                                    songForContextMenu = song
                                    showSongContextMenu = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            if (selectedPlaylist == null) {
                item(key = "songs_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ALL SONGS",
                            color = text,
                            style = adaptiveTypography().labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        SortIcon(
                            mode = songSortMode,
                            onClick = {
                                homeScreenViewModel.onSongSortModeToggle()
                            }
                        )

                        CustomButton(onClick = { isListMode = !isListMode }) {
                            Icon(
                                imageVector = if (isListMode) Icons.Outlined.ViewCarousel else Icons.Outlined.List,
                                contentDescription = if (isListMode) "Switch to carousel view" else "Switch to list view",
                                tint = cardBorder,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }

                if (!isListMode && songFilterQuery.isBlank()) {
                    item(key = "songs_carousel") {
                        MusicCarousel(
                            songs = allSongs,
                            scroll = false,
                            currentPlayingIndex = allSongs.indexOfFirst { it.id == currentSong?.id }
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
                } else {
                    // Rows are part of this one scrolling list, so nothing gets
                    // cut off inside a fixed-height box.
                    val ytResults = (youTubeSearch as? YouTubeSearchState.Results)?.items
                    item(key = "songs_search") {
                        Column {
                            if (listedSongs.isEmpty() && songFilterQuery.isNotBlank()) {
                                when {
                                    youTubeSearch is YouTubeSearchState.Loading ->
                                        LoadingState("Searching YouTube...")
                                    !ytResults.isNullOrEmpty() -> Text(
                                        text = "NOT IN YOUR LIBRARY - FROM YOUTUBE",
                                        color = textSecondary,
                                        style = adaptiveTypography().labelMedium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                    youTubeSearch is YouTubeSearchState.Idle -> Unit // still debouncing
                                    else -> NoSearchResults(
                                        query = songFilterQuery,
                                        onClear = { homeScreenViewModel.onSearchQueryChange("") }
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    if (listedSongs.isEmpty() && !ytResults.isNullOrEmpty()) {
                        items(ytResults, key = { "yt_${it.id}" }) { result ->
                            YouTubeResultRow(
                                result = result,
                                isPreviewing = result.id == previewingId,
                                onPreview = { homeScreenViewModel.togglePreview(result) },
                                onQueue = {
                                    addSongsViewModel.addSingleSong(result.url)
                                    announcements.trySend("Queued \"${result.title}\"")
                                }
                            )
                        }
                    }
                    items(listedSongs, key = { "song_${it.id}" }) { song ->
                        SongListRow(
                            song = song,
                            isPlaying = song.id == currentSong?.id,
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

        if (showAddDialog) {
            AddSongDialog(
                onDismiss = {
                    showAddDialog = false
                    addSongsViewModel.resetState()
                    url = ""
                },
                url = url,
                onUrlChange = { url = it },
                state = addState,
                onSubmit = {
                    if (url.isNotBlank()) {
                        if (LinkDetector.isPlaylist(url)) addSongsViewModel.addPlaylist(url)
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
            // Its own search, so it doesn't share (and leave filtered) the library search box.
            var pickerQuery by remember { mutableStateOf("") }
            val pickerSongs = remember(allSongs, pickerQuery) {
                allSongs.filter {
                    pickerQuery.isBlank() ||
                            it.name.contains(pickerQuery, ignoreCase = true) ||
                            it.artist.contains(pickerQuery, ignoreCase = true)
                }
            }
            val toggleSong: (Song) -> Unit = { song ->
                selectedSongIds = if (song in selectedSongIds) selectedSongIds - song else selectedSongIds + song
            }
            val closePlaylistDialog: () -> Unit = {
                showPlaylistDialog = false
                homeScreenViewModel.resetState()
                selectedSongIds = emptySet()
                selectedPlaylistForDialog = null
                newPlaylistName = ""
            }
            val target = selectedPlaylistForDialog

            if (isAddToExistingMode && target != null) {
                val targetSongs by remember(target.playlist.id) {
                    homeScreenViewModel.songsIn(target.playlist.id)
                }.collectAsState(initial = emptyList())
                AddSongsToPlaylistDialog(
                    playlistName = target.playlist.name,
                    query = pickerQuery,
                    onQueryChange = { pickerQuery = it },
                    songs = pickerSongs,
                    alreadyIn = targetSongs.map { it.id }.toSet(),
                    selected = selectedSongIds,
                    onToggleSong = toggleSong,
                    state = playlistState,
                    onSubmit = { homeScreenViewModel.addSongsToPlaylist(target.playlist, selectedSongIds) },
                    onDismiss = closePlaylistDialog
                )
            } else {
                CreatePlaylistDialog(
                    name = newPlaylistName,
                    onNameChange = { newPlaylistName = it },
                    query = pickerQuery,
                    onQueryChange = { pickerQuery = it },
                    songs = pickerSongs,
                    selected = selectedSongIds,
                    onToggleSong = toggleSong,
                    state = playlistState,
                    onSubmit = { homeScreenViewModel.createPlaylistWithSongs(newPlaylistName.trim(), selectedSongIds) },
                    onDismiss = closePlaylistDialog
                )
            }

            // Close on success and say what happened, instead of a "SAVED"
            // label that left you to press Cancel.
            LaunchedEffect(playlistState) {
                if (playlistState is PlaylistDialogUiState.Success) {
                    announcements.trySend(
                        if (isAddToExistingMode && target != null)
                            "Added ${Formatter.count(selectedSongIds.size, "song")} to ${target.playlist.name}"
                        else "Created ${newPlaylistName.trim()}"
                    )
                    closePlaylistDialog()
                }
            }
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
                    isAddToExistingPlaylistMode = playlists.isNotEmpty()
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
            LaunchedEffect(playlistState) {
                if (playlistState is PlaylistDialogUiState.Success) {
                    announcements.trySend(
                        if (isAddToExistingPlaylistMode)
                            "Added to ${selectedPlaylistForAddSong?.playlist?.name ?: "playlist"}"
                        else "Created ${newPlaylistNameForSong.trim()}"
                    )
                    showAddToPlaylistDialog = false
                    selectedPlaylistForAddSong = null
                    newPlaylistNameForSong = ""
                    songForContextMenu = null
                    isAddToExistingPlaylistMode = false
                    homeScreenViewModel.resetState()
                }
            }
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
                onEndOfSong = { playbackControllerViewModel.startSleepAtEndOfSong() },
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

enum class HomeTab(val label: String) { LIBRARY("LIBRARY"), STATS("STATS") }

/**
 * Top bar: the two places in the app (Library, Stats) as tabs, plus the
 * library actions. Replaces the stats button in the player bar and the
 * floating sync button.
 */
@Composable
private fun AppHeader(
    tab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    onSync: () -> Unit,
    onAddSongs: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .dotGridBackground()
            .statusBarsPadding()
            .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .selectableGroup()
        ) {
            HomeTab.entries.forEach { t ->
                val selected = t == tab
                Column(
                    modifier = Modifier
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onTabSelected(t) }
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = t.label,
                        color = if (selected) text else textSecondary,
                        style = adaptiveTypography().labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .width(if (selected) 28.dp else 0.dp)
                            .background(text, RoundedCornerShape(2.dp))
                    )
                }
            }
        }

        if (tab == HomeTab.LIBRARY) {
            CustomButton(onClick = onSync) {
                Icon(
                    imageVector = Icons.Outlined.Sync,
                    contentDescription = "Sync library",
                    tint = cardBorder,
                    modifier = Modifier.padding(8.dp)
                )
            }
            CustomButton(onClick = onAddSongs, bgColor = primaryAction.copy(alpha = 0.2f)) {
                // Download arrow, not "+", so it can't be confused with New playlist.
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = "Add songs from a link",
                    tint = cardBorder,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

/**
 * Top of an opened playlist: a way back, what it is, and the two things you
 * most likely want to do with it.
 */
@Composable
private fun PlaylistHeader(
    info: PlaylistInfo,
    isReorderMode: Boolean,
    canPlay: Boolean,
    onBack: () -> Unit,
    onToggleReorder: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CustomButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back to library",
                    tint = cardBorder,
                    modifier = Modifier.padding(6.dp)
                )
            }
            Text(
                text = "PLAYLIST",
                color = textSecondary,
                style = adaptiveTypography().labelMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            )
            if (canPlay) {
                CustomButton(
                    onClick = onToggleReorder,
                    bgColor = if (isReorderMode) cardBorder else itemsBg
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Reorder,
                        contentDescription = if (isReorderMode) "Done reordering" else "Reorder songs",
                        tint = if (isReorderMode) itemsBg else cardBorder,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            ShadowBox(modifier = Modifier.size(112.dp), rounding = 5) {
                info.playlist.artPath?.let {
                    AsyncImage(
                        model = File(it),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.playlist.name,
                    color = text,
                    style = adaptiveTypography().titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${Formatter.count(info.numOfSongs, "song")} · " +
                            Formatter.formatTimeToMinutes(info.playlistLength),
                    color = textSecondary,
                    style = adaptiveTypography().bodySmall
                )
                if (canPlay) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        LabeledButton("PLAY", Icons.Outlined.PlayArrow, filled = true, onClick = onPlay)
                        LabeledButton("SHUFFLE", Icons.Outlined.Shuffle, filled = false, onClick = onShuffle)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** Icon + text button in the outlined style; [filled] marks the main action. */
@Composable
private fun LabeledButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    filled: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (filled) itemsBg else cardBorder
    CustomButton(onClick = onClick, bgColor = if (filled) cardBorder else itemsBg) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(
                text = label,
                color = tint,
                style = adaptiveTypography().labelMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

/** Empty state with one clear next step. */
@Composable
private fun EmptyHint(title: String, body: String, action: String, onAction: () -> Unit) {
    EmptyState(
        title = title,
        message = body,
        icon = Icons.Outlined.QueueMusic,
        actionLabel = action,
        onAction = onAction
    )
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
                    text = if (activeCount > 0) "DOWNLOADING · $activeCount" else "DOWNLOADS",
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
                text = job.title
                    ?: if (job.type == DownloadType.PLAYLIST) "Playlist (getting name…)" else "Song (getting name…)",
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

private fun completedMessage(job: DownloadJob): String = when (job.type) {
    DownloadType.PLAYLIST ->
        "Added ${job.title ?: "playlist"} · ${Formatter.count(job.total, "song")}"
    DownloadType.SONG -> "Added ${job.title ?: "song"}"
}

/** Short message in the app's card style, shown above the player bar. */
@Composable
private fun InfoToast(message: String) {
    ShadowBox(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = message,
            color = text,
            style = adaptiveTypography().bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
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
    // Tracknix input: outlined card with a hard shadow, not a Material field.
    Box(modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        OutlinedInput(
            value = query,
            onValueChange = onQueryChange,
            placeholder = placeholder,
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Search, contentDescription = null, tint = cardBorder)
            }
        )
    }
}

/**
 * A YouTube hit. The thumbnail previews the audio; the rest of the row queues
 * the download.
 */
@Composable
private fun YouTubeResultRow(
    result: YouTubeResult,
    isPreviewing: Boolean,
    onPreview: () -> Unit,
    onQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(3.dp, 3.dp)
                .background(cardBorder, shape)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(itemsBg)
                .border(1.5.dp, cardBorder, shape)
                .clickable(onClick = onQueue)
                .padding(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 72.dp, height = 40.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .clickable(onClick = onPreview)
            ) {
                AsyncImage(
                    model = result.thumbnailUrl,
                    contentDescription = "Preview ${result.title}",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = if (isPreviewing) 0.5f else 0.3f))
                ) {
                    Icon(
                        imageVector = if (isPreviewing) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.title,
                    color = text,
                    style = adaptiveTypography().bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(
                        result.artist,
                        result.durationSeconds?.let { "%d:%02d".format(it / 60, it % 60) }
                    ).joinToString(" - "),
                    color = textSecondary,
                    style = adaptiveTypography().bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Outlined.Download,
                contentDescription = "Queue download",
                tint = primaryAction
            )
        }
    }
}

/** Shown in place of the list when a search matches nothing. */
@Composable
private fun NoSearchResults(query: String, onClear: () -> Unit) {
    EmptyState(
        title = "No matches",
        message = "Nothing in your library matches \"$query\". Check the spelling, or search by artist.",
        icon = Icons.Outlined.SearchOff,
        actionLabel = "CLEAR SEARCH",
        onAction = onClear
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongListRow(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Tracknix list item: each row is its own small card (3dp hard shadow,
    // 1.5dp border) so it reads cleanly above the dot grid.
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(3.dp, 3.dp)
                .background(cardBorder, shape)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(itemsBg)
                .border(1.5.dp, cardBorder, shape)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(12.dp)
        ) {
            AsyncImage(
                model = File(song.artPath),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(3.dp)),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = SongDisplay.title(song),
                    color = text,
                    style = adaptiveTypography().bodyMedium,
                    fontWeight = if (isPlaying) FontWeight.ExtraBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isPlaying) "Now playing"
                    else listOfNotNull(
                        SongDisplay.artist(song),
                        Formatter.formatTime(song.length),
                        Formatter.count(playCountOf(song), "play")
                    ).joinToString(" · "),
                    color = textSecondary,
                    style = adaptiveTypography().labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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