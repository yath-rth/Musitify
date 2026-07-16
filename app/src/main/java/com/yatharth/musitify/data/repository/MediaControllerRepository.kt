package com.yatharth.musitify.data.repository

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.music.MusicPlayerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.map
import kotlin.jvm.java

@Singleton
class MediaControllerRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller: StateFlow<MediaController?> = _controller.asStateFlow()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun connect() {
        val sessionToken =
            SessionToken(context, ComponentName(context, MusicPlayerManager::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            val mc = controllerFuture?.get() ?: return@addListener
            _controller.value = mc
            attachListener(mc)
        }, MoreExecutors.directExecutor())
    }

    private fun attachListener(mc: MediaController) {
        mc.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _uiState.update { it.copy(currentTitle = mediaItem?.mediaMetadata?.title?.toString()) }
            }
        })
    }

    fun release() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }

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

    fun play() {
        controller.value?.play()
    }

    fun pause() {
        controller.value?.pause()
    }

    fun skipNext() {
        controller.value?.seekToNext()
    }

    fun skipPrevious() {
        controller.value?.seekToPrevious()
    }
}

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentTitle: String? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0
)