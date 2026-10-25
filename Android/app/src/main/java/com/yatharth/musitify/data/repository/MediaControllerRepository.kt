package com.yatharth.musitify.data.repository

import android.content.ComponentName
import android.content.Context
import android.app.PendingIntent
import android.os.Bundle
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.yatharth.musitify.data.dao.ListeningSessionDao
import com.yatharth.musitify.data.dao.PlaylistDao
import com.yatharth.musitify.data.dao.SongDao
import com.yatharth.musitify.data.entity.ListeningSession
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.music.MusicPlayerManager
import com.yatharth.musitify.service.SleepTimerManager
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
import androidx.core.net.toUri
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
    private val playlistDao: PlaylistDao,
    private val listeningSessionDao: ListeningSessionDao,
    private val sleepTimerManager: SleepTimerManager
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
    private var activePlaylistName: String? = null
    private val songDetailsById = mutableMapOf<Int, Song>()

    val sleepTimerState = sleepTimerManager.state

    /**
     * Builds the queue and starts playback. Also snapshots the songs and the
     * active playlist so listen-time tracking can record denormalized stat
     * rows (names survive library deletions).
     */
    fun startPlayback(songs: List<Song>, startIndex: Int, playlistId: Int?) {
        activePlaylistId = playlistId
        activePlaylistName = null
        songDetailsById.clear()
        songs.forEach { songDetailsById[it.id] = it }

        scope.launch {
            activePlaylistName = playlistId?.let { playlistDao.getPlaylistById(it)?.name }
        }

        val mediaItems = songs.map { song ->
            MediaItem.Builder()
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

    fun connect() {
        val sessionToken =
            SessionToken(context, ComponentName(context, MusicPlayerManager::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            val mc = controllerFuture?.get() ?: return@addListener
            mc.repeatMode = Player.REPEAT_MODE_ALL
            _controller.value = mc
            _uiState.update { it.copy(repeatMode = mc.repeatMode, shuffleMode = mc.shuffleModeEnabled, isPlaying = mc.isPlaying) }
            attachListener(mc)
            setupSleepTimer(mc)
        }, MoreExecutors.directExecutor())
    }

    private fun setupSleepTimer(mc: MediaController) {
        // Sleep timer is set up, and when it completes, pause the player
        // The timer callback is set in startSleepTimer() method
    }

    fun startSleepTimer(duration: com.yatharth.musitify.service.SleepTimerDuration) {
        sleepTimerManager.startTimer(duration) {
            // When timer completes, pause playback
            controller.value?.pause()
        }
    }

    fun startSleepAtEndOfSong() {
        sleepTimerManager.startEndOfSong()
    }

    fun cancelSleepTimer() {
        sleepTimerManager.cancelTimer()
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
                // "End of song" sleep timer: the song finished on its own (or
                // looped), so stop here.
                val songFinished = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                        reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
                if (songFinished && sleepTimerManager.state.value.endOfSong) {
                    mc.pause()
                    sleepTimerManager.cancelTimer()
                }

                flushListenTime()
                lastTrackedSongId =
                    mediaItem?.mediaMetadata?.extras?.getInt("songId")
                accumulatedMs = 0
                lastPositionMs = 0

                _uiState.update {
                    it.copy(
                        currentSongId = lastTrackedSongId,
                        currentTitle = mediaItem?.mediaMetadata?.title?.toString(),
                        positionMs = 0
                    )
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                // Last song ended with repeat off: playback already stopped.
                if (playbackState == Player.STATE_ENDED && sleepTimerManager.state.value.endOfSong) {
                    sleepTimerManager.cancelTimer()
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _uiState.update { it.copy(repeatMode = repeatMode) }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _uiState.update { it.copy(shuffleMode = shuffleModeEnabled) }
            }
        })
    }

    private fun startListenTracking(mc: MediaController) {
        listenTrackingJob?.cancel()
        listenTrackingJob = scope.launch {
            while (isActive) {
                delay(7.seconds)
                val currentPos = mc.currentPosition
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

        val song = songDetailsById[songId]
        val playlistId = activePlaylistId
        val playlistName = activePlaylistName

        scope.launch(Dispatchers.IO) {
            songDao.addListenTime(songId, msToWrite)
            if (playlistId != null) {
                playlistDao.addListenTime(playlistId, msToWrite)
            }
            listeningSessionDao.insert(
                ListeningSession(
                    songId = songId,
                    playlistId = playlistId,
                    songName = song?.name,
                    artistName = song?.artist,
                    playlistName = playlistName,
                    songArtPath = song?.artPath,
                    songLength = song?.length ?: 0,
                    startTimeMs = System.currentTimeMillis() - msToWrite,
                    durationMs = msToWrite
                )
            )
        }
    }

    /** Jumps within the current track. Updates the UI straight away so a
     *  paused player (which isn't polling position) still shows the new spot. */
    fun seekTo(positionMs: Long) {
        val mc = controller.value ?: return
        val target = positionMs.coerceAtLeast(0)
        // Credit what was actually heard before the jump, then measure from the
        // new spot, so skipping ahead isn't counted as listening time.
        if (mc.isPlaying) {
            accumulatedMs += (mc.currentPosition - lastPositionMs).coerceIn(0, 15_000)
        }
        lastPositionMs = target
        mc.seekTo(target)
        _uiState.update { it.copy(positionMs = target) }
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
    val shuffleMode: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0
)