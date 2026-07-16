package com.yatharth.musitify.data.repository

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.music.MusicPlayerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.map
import kotlin.jvm.java
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class MediaControllerRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var positionPollingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
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
                if (isPlaying) startPositionPolling(mc) else stopPositionPolling()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _uiState.update {
                    Log.d("test", mc.duration.toString())
                    it.copy(
                        currentTitle = mediaItem?.mediaMetadata?.title?.toString(),
                        positionMs = 0
                    )
                }
            }
        })
    }

    private fun startPositionPolling(mc: MediaController) {
        positionPollingJob?.cancel()
        positionPollingJob = scope.launch {
            while (isActive) {
                val duration = mc.duration
                _uiState.update {
                    it.copy(
                        positionMs = mc.currentPosition,
                        durationMs = if (duration != C.TIME_UNSET) duration.coerceAtLeast(0) else it.durationMs
                    )
                }
                delay(500)
            }
        }
    }

    private fun stopPositionPolling() {
        positionPollingJob?.cancel()
        positionPollingJob = null
    }

    fun release() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentTitle: String? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0
)