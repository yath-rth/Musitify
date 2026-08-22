package com.yatharth.musitify.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yatharth.musitify.data.repository.PlaylistRepository
import com.yatharth.musitify.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AddSongsUiState {
    object Idle : AddSongsUiState
    data class Loading(val completed: Int = 0, val total: Int = 1) : AddSongsUiState
    data class Success(val count: Int) : AddSongsUiState
    data class Error(val message: String) : AddSongsUiState
}

@HiltViewModel
class AddSongsViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {
    private val _state = MutableStateFlow<AddSongsUiState>(AddSongsUiState.Idle)
    val state: StateFlow<AddSongsUiState> = _state.asStateFlow()

    fun addSingleSong(url: String) {
        viewModelScope.launch {
            _state.value = AddSongsUiState.Loading()
            try {
                songRepository.downloadSong(url)
                _state.value = AddSongsUiState.Success(count = 1)
            } catch (e: Exception) {
                _state.value = AddSongsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun addPlaylist(url: String) {
        viewModelScope.launch {
            _state.value = AddSongsUiState.Loading(completed = 0, total = 1)
            try {
                playlistRepository.downloadPlaylist(
                    playlistUrl = url,
                    onProgress = { completed, total ->
                        _state.value = AddSongsUiState.Loading(completed = completed, total = total)
                    }
                )
            } catch (e: Exception) {
                _state.value = AddSongsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetState() {
        _state.value = AddSongsUiState.Idle
    }
}