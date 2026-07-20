package com.yatharth.musitify.data.viewmodel

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.media3.common.Player
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.repository.MediaControllerRepository
import com.yatharth.musitify.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.core.net.toUri

//TODO: Interact with fillbar to change the position of what you are hearing in the music track

@HiltViewModel
class PlaybackControllerViewModel @Inject constructor(
    private val mediaControllerRepository: MediaControllerRepository
) : ViewModel() {

    val controller = mediaControllerRepository.controller
    val uiState = mediaControllerRepository.uiState

    fun playSongs(songs: List<Song>, startIndex: Int, playlistId: Int?) {
        mediaControllerRepository.setActivePlaylist(playlistId)

        val mediaItems = songs.map { song ->
            androidx.media3.common.MediaItem.Builder()
                .setUri(song.filePath)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(song.name)
                        .setArtworkUri(song.artPath.toUri())
                        .setExtras(Bundle().apply { putInt("songId", song.id) })
                        .build()
                )
                .build()
        }
        controller.value?.setMediaItems(mediaItems, startIndex, 0L)
        controller.value?.prepare()
        controller.value?.play()
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