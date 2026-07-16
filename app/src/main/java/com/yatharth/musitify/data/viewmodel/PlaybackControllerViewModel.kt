package com.yatharth.musitify.data.viewmodel

import androidx.lifecycle.ViewModel
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.repository.MediaControllerRepository
import com.yatharth.musitify.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

//TODO: Interact with fillbar to change the position of what you are hearing in the music track

@HiltViewModel
class PlaybackControllerViewModel @Inject constructor(
    mediaControllerRepository: MediaControllerRepository
) : ViewModel() {

    val controller = mediaControllerRepository.controller
    val uiState = mediaControllerRepository.uiState

    fun playSongs(songs: List<Song>, startIndex: Int) {
        val mediaItems = songs.map { song ->
            androidx.media3.common.MediaItem.Builder()
                .setUri(song.filePath)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(song.name)
                        .build()
                )
                .build()
        }
        controller.value?.setMediaItems(mediaItems, startIndex, 0L)
        controller.value?.prepare()
        controller.value?.play()
    }

    fun pause() {
        if(controller.value?.isPlaying == true) controller.value?.pause()
        else controller.value?.play()
    }

    fun next() {
        controller.value?.seekToNext()
    }

    fun previous() {
        controller.value?.seekToPrevious()
    }

}