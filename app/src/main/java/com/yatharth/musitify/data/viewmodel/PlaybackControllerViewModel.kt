package com.yatharth.musitify.data.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.media3.common.Player
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.repository.MediaControllerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

//TODO: Interact with fillbar to change the position of what you are hearing in the music track

@HiltViewModel
class PlaybackControllerViewModel @Inject constructor(
    private val mediaControllerRepository: MediaControllerRepository
) : ViewModel() {

    val controller = mediaControllerRepository.controller
    val uiState = mediaControllerRepository.uiState

    fun playSongs(songs: List<Song>, startIndex: Int, playlistId: Int?) {
        mediaControllerRepository.startPlayback(songs, startIndex, playlistId)
    }

    fun toggleRepeatMode() {
        controller.value?.let { player ->
            player.repeatMode = when (player.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun pause() {
        if (controller.value?.isPlaying == true) controller.value?.pause()
        else controller.value?.play()
    }

    fun next() {
        controller.value?.seekToNext()
    }

    fun previous() {
        controller.value?.seekToPrevious()
    }

}