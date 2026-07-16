package com.yatharth.musitify.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yatharth.musitify.data.repository.MediaControllerRepository
import com.yatharth.musitify.data.repository.SongRepository
import com.yatharth.musitify.music.MusicPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    songRepository: SongRepository,
    private val mediaControllerRepository: MediaControllerRepository
) : ViewModel() {

    val uiState = mediaControllerRepository.uiState

    val songs = songRepository.allSongs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun playAudio(index: Int) {
        mediaControllerRepository.playSongs(songs.value, startIndex = index)
    }
}