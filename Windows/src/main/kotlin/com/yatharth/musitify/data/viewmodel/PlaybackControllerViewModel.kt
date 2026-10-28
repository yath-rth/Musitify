package com.yatharth.musitify.data.viewmodel

import androidx.lifecycle.ViewModel
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.repository.MediaControllerRepository
import com.yatharth.musitify.data.repository.PlayerUiState
import com.yatharth.musitify.engine.AudioPlayer
import com.yatharth.musitify.engine.SleepTimerDuration
import com.yatharth.musitify.engine.SleepTimerState
import kotlinx.coroutines.flow.StateFlow

/**
 * Port of Android `PlaybackControllerViewModel`. `controller` is the engine [AudioPlayer]
 * (Android: Media3 `MediaController`). Desktop addition: [seekTo].
 */
class PlaybackControllerViewModel(
    private val mediaControllerRepository: MediaControllerRepository
) : ViewModel() {

    val controller: StateFlow<AudioPlayer?> = mediaControllerRepository.controller
    val uiState: StateFlow<PlayerUiState> = mediaControllerRepository.uiState
    val sleepTimerState: StateFlow<SleepTimerState> = mediaControllerRepository.sleepTimerState

    fun playSongs(songs: List<Song>, startIndex: Int, playlistId: Int?) {
        mediaControllerRepository.startPlayback(songs, startIndex, playlistId)
    }

    /** Starts the list at a random song with shuffle on. */
    fun playShuffled(songs: List<Song>, playlistId: Int?) {
        if (songs.isEmpty()) return
        mediaControllerRepository.startPlayback(songs, songs.indices.random(), playlistId)
        controller.value?.setShuffle(true)
    }

    /** OFF(0) → ALL(2) → ONE(1) → OFF. */
    fun toggleRepeatMode() {
        controller.value?.cycleRepeatMode()
    }

    fun toggleShuffleMode() {
        controller.value?.toggleShuffle()
    }

    /** Play/pause toggle (Android name). */
    fun pause() {
        controller.value?.toggle()
    }

    fun next() {
        controller.value?.next()
    }

    /** Restarts the track when past 3 s, otherwise goes to the previous one (Media3 rule). */
    fun previous() {
        controller.value?.previous()
    }

    /** Desktop addition (Android TODO): seek within the current track. */
    fun seekTo(positionMs: Long) {
        controller.value?.seekTo(positionMs)
    }

    fun startSleepTimer(duration: SleepTimerDuration) {
        mediaControllerRepository.startSleepTimer(duration)
    }

    fun startSleepAtEndOfSong() {
        mediaControllerRepository.startSleepAtEndOfSong()
    }

    fun cancelSleepTimer() {
        mediaControllerRepository.cancelSleepTimer()
    }
}
