package com.yatharth.musitify.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Reorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.yatharth.musitify.ui.elements.ArtImage
import com.yatharth.musitify.ui.elements.BackHandler
import com.yatharth.musitify.ui.elements.CustomAlertBox
import com.yatharth.musitify.ui.elements.CustomCircularButton
import com.yatharth.musitify.ui.elements.PlaybackControls
import com.yatharth.musitify.ui.elements.PlaylistContextMenuDialog
import com.yatharth.musitify.ui.elements.PlaylistDialog
import com.yatharth.musitify.ui.elements.RenamePlaylistDialog
import com.yatharth.musitify.ui.elements.RenameSongDialog
import com.yatharth.musitify.ui.elements.ReorderableSongList
import com.yatharth.musitify.ui.elements.SettingsDialog
import com.yatharth.musitify.ui.elements.ShadowBox
import com.yatharth.musitify.ui.elements.SleepTimerDialog
import com.yatharth.musitify.ui.elements.SongContextMenuDialog
import com.yatharth.musitify.ui.elements.SortIcon
import com.yatharth.musitify.ui.elements.display.MusicCarousel
import com.yatharth.musitify.ui.elements.display.PlaylistCarousel
import com.yatharth.musitify.ui.elements.handCursor
import com.yatharth.musitify.ui.elements.onRightClickOrLongPress
import com.yatharth.musitify.ui.elements.trackTextFieldFocus
import com.yatharth.musitify.ui.theme.accentColor1
import com.yatharth.musitify.ui.theme.accentColor2
import com.yatharth.musitify.ui.theme.background
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.text
import com.yatharth.musitify.util.Formatter
import com.yatharth.musitify.util.SongMedia.playCountOf
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

/** Window width from which the vinyl moves into a permanent right-side panel. */
private val WideLayoutMinWidth = 1100.dp
private val SidePanelWidth = 420.dp
private const val MESSAGE_DURATION_MS = 4_000L

/**
 * Home: playlists carousel (or the open playlist's songs), All Songs (carousel or
 * searchable list), download queue and the spinning vinyl, with PlaybackControls at
 * the bottom — 1:1 with Android.
 *
 * Desktop: on windows ≥ 1100 dp the vinyl lives in a 420 dp right panel (visible while
 * a song is loaded); the header gains "+" (add songs; right click = create playlist —
 * Android's commented-out button) and a settings gear; a back arrow leaves a playlist
 * (Esc too); right click = long press; HomeScreenViewModel.message shows as a toast.
 */
@Composable
fun HomeScreen(
    modifier: Modifier,
    homeScreenViewModel: HomeScreenViewModel = koinViewModel(),
    playbackControllerViewModel: PlaybackControllerViewModel = koinViewModel(),
    addSongsViewModel: AddSongsViewModel = koinViewModel(),
    onOpenStats: () -> Unit = {}
) {
    val allSongs by homeScreenViewModel.sortedSongs.collectAsState()
    val filteredSongs by homeScreenViewModel.searchResults.collectAsState()
    val uiState by playbackControllerViewModel.uiState.collectAsState()
    val addState by addSongsViewModel.state.collectAsState()
    val downloadJobs by addSongsViewModel.jobs.collectAsState()
    val songSortMode by homeScreenViewModel.songSortMode.collectAsState()
    val sleepTimerState by playbackControllerViewModel.sleepTimerState.collectAsState()
    val message by homeScreenViewModel.message.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
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

    // Android keyed this only on the id (stale name after a rename); keyed on both here.
    val selectedPlaylist: PlaylistInfo? = remember(selectedPlaylistId, playlists) {
        playlists.firstOrNull { it.playlist.id == selectedPlaylistId }
    }

    // Android passed the playlist *id* as the carousel index. Desktop passes the index and
    // remembers the last opened playlist so leaving it lands back on the same card.
    var lastPlaylistIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(selectedPlaylistId, playlists) {
        val index = playlists.indexOfFirst { it.playlist.id == selectedPlaylistId }
        if (index >= 0) lastPlaylistIndex = index
    }

    // Song for the vinyl: by id first, then by name (Android matched by name and crashed on
    // an empty library).
    val nowPlayingSong: Song? = uiState.currentTitle?.let { title ->
        allSongs.firstOrNull { it.id == uiState.currentSongId }
            ?: allSongs.firstOrNull { it.name == title }
    }

    // Once a URL is queued, close the dialog; the queue card shows progress from here.
    LaunchedEffect(addState) {
        if (addState is AddSongsUiState.Queued) {
            showAddDialog = false
            url = ""
            addSongsViewModel.resetState()
        }
    }

    LaunchedEffect(message) {
        if (message != null) {
            delay(MESSAGE_DURATION_MS)
            homeScreenViewModel.clearMessage()
        }
    }

    BackHandler(enabled = songsInPlaylist.isNotEmpty()) {
        homeScreenViewModel.clearSelectedPlaylist()
    }

    val openSongMenu: (Song) -> Unit = { song ->
        songForContextMenu = song
        showSongContextMenu = true
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= WideLayoutMinWidth

        Scaffold(
            modifier = Modifier.fillMaxSize(),
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
                        onShuffleToggle = { playbackControllerViewModel.toggleShuffleMode() },
                        addSong = { showAddDialog = true },
                        createPlaylist = { showPlaylistDialog = true },
                        onStats = onOpenStats,
                        onSleepTimer = { showSleepTimerDialog = true },
                        sleepTimerActive = sleepTimerState.isActive,
                        onSeek = { playbackControllerViewModel.seekTo(it) }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                // A Box (not a Row) so RowScope doesn't leak into the LazyColumn's items.
                val showSidePanel = wide && nowPlayingSong != null
                Box(modifier = Modifier.fillMaxSize()) {
                    val listState = rememberLazyListState()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = if (showSidePanel) SidePanelWidth else 0.dp)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = modifier
                                .fillMaxSize()
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

                            item(key = "playlists") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (selectedPlaylist != null) {
                                            IconButton(
                                                onClick = { homeScreenViewModel.clearSelectedPlaylist() },
                                                modifier = Modifier.handCursor()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                                    contentDescription = "Back to playlists",
                                                    tint = cardBorder
                                                )
                                            }
                                        }
                                        Text(
                                            text = selectedPlaylist?.playlist?.name?.uppercase() ?: "PLAYLISTS",
                                            color = text,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 2.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (selectedPlaylist == null) {
                                            SortIcon(
                                                mode = playlistSortMode,
                                                onClick = { homeScreenViewModel.onPlaylistSortModeToggle() }
                                            )
                                        } else {
                                            // Show reorder button when a playlist is selected
                                            IconButton(
                                                onClick = { isReorderMode = !isReorderMode },
                                                modifier = Modifier.handCursor()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Reorder,
                                                    contentDescription = if (isReorderMode) "Exit reorder mode" else "Reorder songs",
                                                    tint = if (isReorderMode) accentColor2 else cardBorder
                                                )
                                            }
                                        }
                                        CustomCircularButton(
                                            onClick = { showAddDialog = true },
                                            onLongClick = { showPlaylistDialog = true }
                                        ) {
                                            Icon(Icons.Outlined.Add, contentDescription = "Add songs", tint = cardBorder)
                                        }
                                        CustomCircularButton(onClick = { showSettingsDialog = true }) {
                                            Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = cardBorder)
                                        }
                                    }
                                }

                                AnimatedVisibility(visible = songsInPlaylist.isEmpty()) {
                                    PlaylistCarousel(
                                        playlists = playlists,
                                        currentPlayingIndex = lastPlaylistIndex,
                                        onPlaylistSelected = { id -> homeScreenViewModel.loadPlaylist(id) },
                                        onLongClick = { playlist ->
                                            playlistForContextMenu = playlist
                                            showPlaylistContextMenu = true
                                        },
                                        onCreatePlaylist = { showPlaylistDialog = true },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                selectedPlaylistId?.let { playlistId ->
                                    val playingIndex = songsInPlaylist
                                        .indexOfFirst { it.name == uiState.currentTitle }
                                        .coerceAtLeast(0)

                                    AnimatedVisibility(visible = songsInPlaylist.isNotEmpty() && !isReorderMode) {
                                        MusicCarousel(
                                            songs = songsInPlaylist,
                                            currentPlayingIndex = playingIndex,
                                            onTrackSelected = { index ->
                                                playbackControllerViewModel.playSongs(songsInPlaylist, index, playlistId)
                                            },
                                            onLongSelect = openSongMenu,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    AnimatedVisibility(visible = songsInPlaylist.isNotEmpty() && isReorderMode) {
                                        Column {
                                            Text(
                                                text = "USE ARROWS TO REORDER SONGS",
                                                color = accentColor2,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp,
                                                modifier = Modifier
                                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                            )
                                            ReorderableSongList(
                                                songs = songsInPlaylist,
                                                currentPlayingIndex = playingIndex,
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
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            item(key = "all_songs") {
                                AnimatedVisibility(visible = songsInPlaylist.isEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ALL SONGS",
                                            color = text,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 2.sp,
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        )

                                        SortIcon(
                                            mode = songSortMode,
                                            onClick = { homeScreenViewModel.onSongSortModeToggle() }
                                        )

                                        IconButton(
                                            onClick = { isListMode = !isListMode },
                                            modifier = Modifier.handCursor()
                                        ) {
                                            Icon(
                                                imageVector = if (isListMode) Icons.Outlined.ViewCarousel else Icons.AutoMirrored.Outlined.List,
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
                                            playbackControllerViewModel.playSongs(allSongs, index, null)
                                        },
                                        onLongSelect = openSongMenu,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                AnimatedVisibility(visible = songsInPlaylist.isEmpty() && isListMode) {
                                    ShadowBox(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 500.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            LibrarySearchBar(
                                                query = songFilterQuery,
                                                onQueryChange = { homeScreenViewModel.onSearchQueryChange(it) },
                                                placeholder = "search your library"
                                            )
                                            LazyColumn(Modifier.padding(vertical = 12.dp)) {
                                                val listedSongs =
                                                    if (songFilterQuery.isBlank()) allSongs else filteredSongs
                                                items(listedSongs, key = { it.id }) { song ->
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
                                                        // Never fired on Android; desktop opens the song menu.
                                                        onLongClick = { openSongMenu(song) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (!wide) {
                                item(key = "now_playing") {
                                    nowPlayingSong?.let { song ->
                                        PlaybackScreen(
                                            modifier = Modifier.fillMaxWidth(),
                                            song = song,
                                            playing = uiState.isPlaying
                                        )
                                    }
                                }
                            }
                        }

                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(listState),
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                        )
                    }

                    // ---- Wide windows: vinyl in a permanent side panel ----
                    if (wide) {
                        AnimatedVisibility(
                            visible = showSidePanel,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(SidePanelWidth)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState())
                                    .padding(top = 8.dp, end = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                nowPlayingSong?.let { song ->
                                    PlaybackScreen(
                                        modifier = Modifier.fillMaxWidth(),
                                        song = song,
                                        playing = uiState.isPlaying
                                    )
                                }
                            }
                        }
                    }
                }

                MessageToast(
                    message = message,
                    onDismiss = { homeScreenViewModel.clearMessage() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }
    }

    // ---------------------------------------------------------------- dialogs

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
                        homeScreenViewModel.deleteSongFromPlaylist(index = index, playlistId = playlistId)
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
                selectedSongIds = if (selectedSongIds.contains(song)) selectedSongIds - song else selectedSongIds + song
            },
            state = playlistState,
            onSubmit = {
                if (isAddToExistingMode) {
                    selectedPlaylistForDialog?.let {
                        homeScreenViewModel.addSongsToPlaylist(it.playlist, selectedSongIds)
                    }
                } else {
                    homeScreenViewModel.createPlaylistWithSongs(newPlaylistName, selectedSongIds)
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
                        homeScreenViewModel.addSongsToPlaylist(it.playlist, setOf(songForContextMenu!!))
                    }
                } else if (newPlaylistNameForSong.isNotBlank()) {
                    homeScreenViewModel.createPlaylistWithSongs(newPlaylistNameForSong, setOf(songForContextMenu!!))
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
            onSetTimer = { duration -> playbackControllerViewModel.startSleepTimer(duration) },
            onCancelTimer = { playbackControllerViewModel.cancelSleepTimer() },
            onDismiss = { showSleepTimerDialog = false }
        )
    }

    // Desktop: settings (gear in the header)
    if (showSettingsDialog) {
        SettingsDialog(onDismiss = { showSettingsDialog = false })
    }
}

/**
 * Tracknix-styled toast for [HomeScreenViewModel.message] (errors of background actions,
 * sync results). Auto-hides after a few seconds; × closes it immediately.
 */
@Composable
private fun MessageToast(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keep the last text while the exit animation runs.
    var lastMessage by remember { mutableStateOf("") }
    if (message != null) lastMessage = message

    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        ShadowBox(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .padding(end = 4.dp, bottom = 4.dp),
            rounding = 10
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 24.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .drawBehind { drawRect(accentColor1) }
                )
                Text(
                    text = lastMessage,
                    color = text,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.handCursor()) {
                    Icon(Icons.Outlined.Close, contentDescription = "Dismiss", tint = cardBorder)
                }
            }
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
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
                if (hasFinished) {
                    TextButton(onClick = onClearFinished, modifier = Modifier.handCursor()) {
                        Text(
                            text = "Clear",
                            color = cardBorder,
                            style = MaterialTheme.typography.labelMedium
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
                    modifier = Modifier.align(Alignment.CenterHorizontally).handCursor()
                ) {
                    Text(
                        text = if (expanded) "Show less" else "Show $hiddenCount more",
                        color = cardBorder,
                        style = MaterialTheme.typography.labelMedium
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

    val statusColor = if (job.status == DownloadStatus.FAILED) text else text.copy(alpha = 0.6f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = (if (job.type == DownloadType.PLAYLIST) "Playlist · " else "Song · ") + displayUrl(job.url),
                color = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = statusText,
                color = statusColor,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (job.status == DownloadStatus.RUNNING) {
                Spacer(modifier = Modifier.height(6.dp))
                // Desktop: single songs also report yt-dlp progress (job.progress).
                val fraction: Float? = when {
                    job.total > 0 -> (job.completed.toFloat() / job.total).coerceIn(0f, 1f)
                    job.progress != null -> job.progress.coerceIn(0f, 1f)
                    else -> null
                }
                if (fraction != null) {
                    LinearProgressIndicator(
                        progress = { fraction },
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
            job.isActive -> IconButton(onClick = onCancel, modifier = Modifier.handCursor()) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Cancel download",
                    tint = cardBorder
                )
            }
            job.status == DownloadStatus.FAILED || job.status == DownloadStatus.CANCELLED ->
                IconButton(onClick = onRetry, modifier = Modifier.handCursor()) {
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
 * row list so finding something in a large library doesn't rely on scroll position.
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
                style = MaterialTheme.typography.bodyMedium,
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
            .trackTextFieldFocus()
    )
}

/** A single row in the dense All Songs list. Desktop: hover tint, right click = long press. */
@Composable
private fun SongListRow(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hoverSource = remember { MutableInteractionSource() }
    val hovered by hoverSource.collectIsHoveredAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .hoverable(hoverSource)
            .drawBehind { if (hovered) drawRect(cardBorder.copy(alpha = 0.05f)) }
            .handCursor()
            .clickable(onClick = onClick)
            .onRightClickOrLongPress(action = onLongClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        ArtImage(
            path = song.artPath,
            contentDescription = song.name,
            modifier = Modifier
                .size(34.dp)
                .aspectRatio(1f)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.name,
                color = if (isPlaying) cardBorder else text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${Formatter.formatTime(song.length)} · ${playCountOf(song)} plays",
                color = text.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
