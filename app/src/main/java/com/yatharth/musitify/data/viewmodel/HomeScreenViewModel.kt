package com.yatharth.musitify.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yatharth.musitify.data.entity.Playlist
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.repository.PlaylistRepository
import com.yatharth.musitify.data.repository.SongRepository
import com.yatharth.musitify.network.ActionController
import com.yatharth.musitify.network.SyncController
import com.yatharth.musitify.ui.elements.SortMode
import com.yatharth.musitify.util.SongMedia.playCountOf
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository,
    private val syncController: SyncController,
    private val actionController: ActionController
) : ViewModel() {

    val allSongs = songRepository.allSongs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val playlists = playlistRepository.allPlaylists.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _playlistId = MutableStateFlow<Int?>(null)
    val playlistId = _playlistId.asStateFlow()

    val songsInPlaylist = _playlistId
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
            } catch (e: Exception) {
                _state.value = PlaylistDialogUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private val _songSortMode = MutableStateFlow(SortMode.PLAYS_DESC)
    val songSortMode = _songSortMode.asStateFlow()

    fun onSongSortModeToggle() {
        _songSortMode.value = _songSortMode.value.next()
    }


    private val _playlistSortMode = MutableStateFlow(SortMode.PLAYS_DESC)
    val playlistSortMode = _playlistSortMode.asStateFlow()

    fun onPlaylistSortModeToggle() {
        _playlistSortMode.value = _playlistSortMode.value.next()
    }

    val sortedSongs = combine(allSongs, _songSortMode) { songs, mode ->
        when (mode) {
            SortMode.DOWNLOADED_DESC -> songs.sortedByDescending { it.downloadedAt }
            SortMode.DOWNLOADED_ASC -> songs.sortedBy { it.downloadedAt }
            SortMode.PLAYS_DESC -> songs.sortedByDescending { playCountOf(it) }
            SortMode.PLAYS_ASC -> songs.sortedBy { playCountOf(it) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sortedPlaylists = combine(playlists, _playlistSortMode) { playlists, mode ->
        when (mode) {
            SortMode.DOWNLOADED_DESC -> playlists.sortedByDescending { it.playlist.createdAt }
            SortMode.DOWNLOADED_ASC -> playlists.sortedBy { it.playlist.createdAt }
            SortMode.PLAYS_DESC -> playlists.sortedByDescending { it.playlist.timeHeard }
            SortMode.PLAYS_ASC -> playlists.sortedBy { it.playlist.timeHeard }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchResults = _searchQuery
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

    fun deleteSong(index: Int) {
        viewModelScope.launch {
            songRepository.deleteSongById(id = index)
        }
    }

    fun deleteSongFromPlaylist(index: Int, playlistId: Int) {
        viewModelScope.launch {
            playlistRepository.removeSong(songId = index, playlistId = playlistId)
        }
    }

    fun renameSong(song: Song, newName: String) {
        viewModelScope.launch {
            songRepository.updateSong(song.copy(name = newName))
        }
    }

    fun deletePlaylist(index: Int) {
        viewModelScope.launch {
            playlistRepository.deletePlaylistById(id = index)
        }
    }

    fun syncLibrary() {
        viewModelScope.launch {
//            syncController.sendData()
            actionController.pushActions()
            actionController.pullActions()
        }
    }

    fun fetchLibrary() {
        viewModelScope.launch {
//            syncController.receiveData()
            actionController.pullActions()
        }
    }
}

sealed interface PlaylistDialogUiState {
    object Idle : PlaylistDialogUiState
    object Loading : PlaylistDialogUiState
    object Success : PlaylistDialogUiState
    data class Error(val message: String) : PlaylistDialogUiState
}
