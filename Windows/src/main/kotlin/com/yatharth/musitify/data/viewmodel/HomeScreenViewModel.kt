package com.yatharth.musitify.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yatharth.musitify.data.entity.Playlist
import com.yatharth.musitify.data.entity.PlaylistInfo
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.repository.PlaylistRepository
import com.yatharth.musitify.data.repository.SongRepository
import com.yatharth.musitify.data.repository.SyncGateway
import com.yatharth.musitify.ui.elements.SortMode
import com.yatharth.musitify.util.SongMedia.playCountOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Port of Android `HomeScreenViewModel`. Android's SyncController/ActionController are replaced by
 * [SyncGateway] (bootstrap sync is commented out on Android anyway).
 *
 * Desktop addition: failures of fire-and-forget actions (delete/rename/remove/sync), which crash
 * the app on Android, are caught and published in [message] (clear with [clearMessage]).
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HomeScreenViewModel(
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository,
    private val actionController: SyncGateway,
) : ViewModel() {

    val allSongs: StateFlow<List<Song>> = songRepository.allSongs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val playlists: StateFlow<List<PlaylistInfo>> = playlistRepository.allPlaylists.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _playlistId = MutableStateFlow<Int?>(null)
    val playlistId: StateFlow<Int?> = _playlistId.asStateFlow()

    val songsInPlaylist: StateFlow<List<Song>> = _playlistId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(emptyList())
            } else {
                playlistRepository.songsInPlaylist(id)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun loadPlaylist(id: Int) {
        _playlistId.value = id
    }

    fun clearSelectedPlaylist() {
        _playlistId.value = null
    }

    private val _state = MutableStateFlow<PlaylistDialogUiState>(PlaylistDialogUiState.Idle)
    val state: StateFlow<PlaylistDialogUiState> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun createPlaylistWithSongs(name: String, songs: Set<Song>) {
        if (name.isBlank() || songs.isEmpty()) {
            _state.value = PlaylistDialogUiState.Error("Name and at least one song required")
            return
        }
        viewModelScope.launch {
            _state.value = PlaylistDialogUiState.Loading
            try {
                playlistRepository.createPlaylistAndAddSongs(name, songs.associateBy { it.id })
                _state.value = PlaylistDialogUiState.Success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = PlaylistDialogUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun addSongsToPlaylist(playlist: Playlist, songs: Set<Song>) {
        if (songs.isEmpty()) {
            _state.value = PlaylistDialogUiState.Error("Select at least one song")
            return
        }
        viewModelScope.launch {
            _state.value = PlaylistDialogUiState.Loading
            try {
                playlistRepository.addSongsToPlaylist(playlist, songs.associateBy { it.id })
                _state.value = PlaylistDialogUiState.Success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = PlaylistDialogUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private val _songSortMode = MutableStateFlow(SortMode.PLAYS_DESC)
    val songSortMode: StateFlow<SortMode> = _songSortMode.asStateFlow()

    fun onSongSortModeToggle() {
        _songSortMode.value = _songSortMode.value.next()
    }

    private val _playlistSortMode = MutableStateFlow(SortMode.PLAYS_DESC)
    val playlistSortMode: StateFlow<SortMode> = _playlistSortMode.asStateFlow()

    fun onPlaylistSortModeToggle() {
        _playlistSortMode.value = _playlistSortMode.value.next()
    }

    val sortedSongs: StateFlow<List<Song>> = combine(allSongs, _songSortMode) { songs, mode ->
        when (mode) {
            SortMode.DOWNLOADED_DESC -> songs.sortedByDescending { it.downloadedAt }
            SortMode.DOWNLOADED_ASC -> songs.sortedBy { it.downloadedAt }
            SortMode.PLAYS_DESC -> songs.sortedByDescending { playCountOf(it) }
            SortMode.PLAYS_ASC -> songs.sortedBy { playCountOf(it) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sortedPlaylists: StateFlow<List<PlaylistInfo>> = combine(playlists, _playlistSortMode) { playlists, mode ->
        when (mode) {
            SortMode.DOWNLOADED_DESC -> playlists.sortedByDescending { it.playlist.createdAt }
            SortMode.DOWNLOADED_ASC -> playlists.sortedBy { it.playlist.createdAt }
            SortMode.PLAYS_DESC -> playlists.sortedByDescending { it.playlist.timeHeard }
            SortMode.PLAYS_ASC -> playlists.sortedBy { it.playlist.timeHeard }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Song>> = _searchQuery
        .debounce(300.milliseconds)
        .flatMapLatest { query ->
            if (query.isBlank()) songRepository.allSongs
            else songRepository.getFilteredSongs(query)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun resetState() {
        _state.value = PlaylistDialogUiState.Idle
    }

    /** [index] is the song **id** (Android naming). */
    fun deleteSong(index: Int) {
        launchReporting { songRepository.deleteSongById(id = index) }
    }

    fun deleteSongFromPlaylist(index: Int, playlistId: Int) {
        launchReporting { playlistRepository.removeSong(songId = index, playlistId = playlistId) }
    }

    fun renameSong(song: Song, newName: String) {
        launchReporting { songRepository.updateSong(song.copy(name = newName)) }
    }

    fun renamePlaylist(playlist: Playlist, newName: String) {
        launchReporting { playlistRepository.updatePlaylist(playlist.copy(name = newName)) }
    }

    /** [index] is the playlist **id** (Android naming). */
    fun deletePlaylist(index: Int) {
        launchReporting { playlistRepository.deletePlaylistById(id = index) }
    }

    fun syncLibrary() {
        launchReporting {
            actionController.pushActions()
            actionController.pullActions()
        }
    }

    fun fetchLibrary() {
        launchReporting { actionController.pullActions() }
    }

    fun reorderSongsInPlaylist(fromIndex: Int, toIndex: Int) {
        val playlistId = _playlistId.value ?: return
        launchReporting { playlistRepository.reorderSongsInPlaylist(playlistId, fromIndex, toIndex) }
    }

    private fun launchReporting(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _message.value = e.message ?: "Unknown error"
            }
        }
    }
}

sealed interface PlaylistDialogUiState {
    object Idle : PlaylistDialogUiState
    object Loading : PlaylistDialogUiState
    object Success : PlaylistDialogUiState
    data class Error(val message: String) : PlaylistDialogUiState
}
