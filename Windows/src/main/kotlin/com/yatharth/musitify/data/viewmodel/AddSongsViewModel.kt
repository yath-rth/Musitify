package com.yatharth.musitify.data.viewmodel

import androidx.lifecycle.ViewModel
import com.yatharth.musitify.engine.DownloadJob
import com.yatharth.musitify.engine.DownloadQueue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AddSongsUiState {
    object Idle : AddSongsUiState
    data class Queued(val isPlaylist: Boolean) : AddSongsUiState
    data class Error(val message: String) : AddSongsUiState
}

/** Port of Android `AddSongsViewModel`; Android's DownloadsManager is the engine [DownloadQueue]. */
class AddSongsViewModel(
    private val downloadsManager: DownloadQueue
) : ViewModel() {

    private val _state = MutableStateFlow<AddSongsUiState>(AddSongsUiState.Idle)
    val state: StateFlow<AddSongsUiState> = _state.asStateFlow()

    val jobs: StateFlow<List<DownloadJob>> = downloadsManager.jobs

    fun addSingleSong(url: String) {
        if (url.isBlank()) {
            _state.value = AddSongsUiState.Error("URL is empty")
            return
        }
        _state.value =
            if (downloadsManager.enqueueSong(url)) AddSongsUiState.Queued(isPlaylist = false)
            else AddSongsUiState.Error("Already in the queue")
    }

    fun addPlaylist(url: String) {
        if (url.isBlank()) {
            _state.value = AddSongsUiState.Error("URL is empty")
            return
        }
        _state.value =
            if (downloadsManager.enqueuePlaylist(url)) AddSongsUiState.Queued(isPlaylist = true)
            else AddSongsUiState.Error("Already in the queue")
    }

    fun cancel(id: Long) = downloadsManager.cancel(id)
    fun retry(id: Long) = downloadsManager.retry(id)
    fun clearFinished() = downloadsManager.clearFinished()

    fun resetState() {
        _state.value = AddSongsUiState.Idle
    }
}
