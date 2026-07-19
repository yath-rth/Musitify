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
import com.yatharth.musitify.data.dao.PlaylistDao
import com.yatharth.musitify.data.dao.SongDao
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
import kotlin.time.Duration.Companion.seconds

@Singleton
class MediaControllerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var positionPollingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller: StateFlow<MediaController?> = _controller.asStateFlow()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var listenTrackingJob: Job? = null
    private var lastTrackedSongId: Int? = null
    private var accumulatedMs: Long = 0
    private var lastPositionMs: Long = 0

    private var activePlaylistId: Int? = null

    fun setActivePlaylist(playlistId: Int?) {
        activePlaylistId = playlistId
    }

    fun connect() {
        val sessionToken =
            SessionToken(context, ComponentName(context, MusicPlayerManager::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            val mc = controllerFuture?.get() ?: return@addListener
            _controller.value = mc
            _uiState.update { it.copy(repeatMode = mc.repeatMode, isPlaying = mc.isPlaying) }
            attachListener(mc)
        }, MoreExecutors.directExecutor())
    }

    private fun attachListener(mc: MediaController) {
        mc.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startPositionPolling(mc)
                    startListenTracking(mc)
                } else {
                    stopPositionPolling()
                    flushListenTime()
                    stopListenTracking()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                flushListenTime()
                lastTrackedSongId =
                    mediaItem?.mediaMetadata?.extras?.getInt("songId") // see note below
                accumulatedMs = 0
                lastPositionMs = 0

                _uiState.update {
                    it.copy(
                        currentTitle = mediaItem?.mediaMetadata?.title?.toString(),
                        positionMs = 0
                    )
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _uiState.update { it.copy(repeatMode = repeatMode) }
            }
        })
    }

    private fun startListenTracking(mc: MediaController) {
        listenTrackingJob?.cancel()
        listenTrackingJob = scope.launch {
            while (isActive) {
                delay(7.seconds) // your 5-10s window, pick a number
                val currentPos = mc.currentPosition
                // guard against seeks/skips inflating the delta
                val delta = (currentPos - lastPositionMs).coerceIn(0, 15_000)
                accumulatedMs += delta
                lastPositionMs = currentPos
                flushListenTime()
            }
        }
    }

    private fun stopListenTracking() {
        listenTrackingJob?.cancel()
        listenTrackingJob = null
    }

    private fun flushListenTime() {
        val songId = lastTrackedSongId ?: return
        if (accumulatedMs <= 0) return

        val msToWrite = accumulatedMs
        accumulatedMs = 0

        scope.launch(Dispatchers.IO) {
            songDao.addListenTime(songId, msToWrite)
            activePlaylistId?.let { playlistDao.addListenTime(it, msToWrite) }
        }
    }

    fun release() {
        flushListenTime()
        controllerFuture?.let { MediaController.releaseFuture(it) }
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
                delay(500.milliseconds)
            }
        }
    }

    private fun stopPositionPolling() {
        positionPollingJob?.cancel()
        positionPollingJob = null
    }
}

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentSongId: Int? = null,
    val currentTitle: String? = null,
    val repeatMode: Int = 0,
    val positionMs: Long = 0,
    val durationMs: Long = 0
)