package com.yatharth.musitify.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yatharth.musitify.data.entity.Playlist
import com.yatharth.musitify.data.repository.PlaylistRepository
import com.yatharth.musitify.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    val songs = songRepository.allSongs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val playlists = playlistRepository.allPlaylists.stateIn(
        viewModelScope,
        kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _state = MutableStateFlow<PlaylistDialogUiState>(PlaylistDialogUiState.Idle)
    val state: StateFlow<PlaylistDialogUiState> = _state.asStateFlow()

    fun createPlaylistWithSongs(name: String, songIds: Set<Int>) {
        if (name.isBlank() || songIds.isEmpty()) {
            _state.value = PlaylistDialogUiState.Error("Name and at least one song required")
            return
        }
        viewModelScope.launch {
            _state.value = PlaylistDialogUiState.Loading
            try {
                playlistRepository.createPlaylist(name, songIds.toList())
                _state.value = PlaylistDialogUiState.Success
            } catch (e: Exception) {
                _state.value = PlaylistDialogUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun addSongsToPlaylist(playlist: Playlist, songIds: Set<Int>) {
        if (songIds.isEmpty()) {
            _state.value = PlaylistDialogUiState.Error("Select at least one song")
            return
        }
        viewModelScope.launch {
            _state.value = PlaylistDialogUiState.Loading
            try {
                playlistRepository.addSongsToPlaylist(playlist.id, songIds.toList())
                _state.value = PlaylistDialogUiState.Success
            } catch (e: Exception) {
                _state.value = PlaylistDialogUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetState() {
        _state.value = PlaylistDialogUiState.Idle
    }
}

sealed interface PlaylistDialogUiState {
    object Idle : PlaylistDialogUiState
    object Loading : PlaylistDialogUiState
    object Success : PlaylistDialogUiState
    data class Error(val message: String) : PlaylistDialogUiState
}
