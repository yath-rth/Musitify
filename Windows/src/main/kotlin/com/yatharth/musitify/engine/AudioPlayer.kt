package com.yatharth.musitify.engine

import javafx.application.Platform
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** A queue item. Plain data so the engine stays independent of Room entities. */
data class Track(
    val id: Long,
    val path: String,
    val title: String,
    val artist: String,
    val artPath: String?,
    val durationMs: Long,
)

data class PlayerState(
    val isPlaying: Boolean = false,
    val currentIndex: Int = -1,
    val currentTrack: Track? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val repeatMode: RepeatMode = RepeatMode.ALL, // Android sets REPEAT_MODE_ALL on connect
    val shuffleEnabled: Boolean = false,
    val volume: Double = 1.0,
    val queue: List<Track> = emptyList(),
    /** Repeat OFF and the last item finished. */
    val ended: Boolean = false,
    /** Last media error (e.g. missing/undecodable file), cleared on next successful load. */
    val error: String? = null,
)

/** Starts the JavaFX toolkit once (without a JavaFX window) and keeps it alive. */
object FxToolkit {
    @Volatile
    private var started = false

    @Synchronized
    fun ensureStarted() {
        if (started) return
        val latch = CountDownLatch(1)
        try {
            Platform.startup { latch.countDown() }
            latch.await(10, TimeUnit.SECONDS)
        } catch (_: IllegalStateException) {
            // Toolkit already initialized (e.g. by a JFXPanel)
        }
        Platform.setImplicitExit(false)
        started = true
    }

    fun runOnFx(block: () -> Unit) {
        ensureStarted()
        if (Platform.isFxApplicationThread()) block() else Platform.runLater(block)
    }
}

/**
 * JavaFX [MediaPlayer] wrapper with an ExoPlayer-like queue (see [QueueNavigator]).
 * Public API is thread-safe: every call is marshalled onto the FX thread, and all mutable
 * player state is confined to it. [state] is refreshed every 500 ms.
 */
class AudioPlayer(
    private val pollIntervalMs: Long = 500,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _transitions = MutableSharedFlow<Track>(extraBufferCapacity = 64)

    /** Emits on every media-item change, including ONE-repeat restarts (Media3 `onMediaItemTransition`). */
    val transitions: SharedFlow<Track> = _transitions.asSharedFlow()

    val queue: List<Track> get() = _state.value.queue

    /**
     * When true, the current track plays to its end and playback then pauses (the queue still
     * moves on to the next item, paused) — the "end of song" sleep timer. Reset after it fires.
     */
    @Volatile
    var stopAfterCurrentTrack: Boolean = false

    private val _stoppedAtTrackEnd = MutableSharedFlow<Unit>(extraBufferCapacity = 8)

    /** Emits when [stopAfterCurrentTrack] paused playback at the end of a track. */
    val stoppedAtTrackEnd: SharedFlow<Unit> = _stoppedAtTrackEnd.asSharedFlow()

    // ---- FX-thread confined ----
    private var player: MediaPlayer? = null
    private var tracks: List<Track> = emptyList()
    private var index = -1
    private val navigator = QueueNavigator()
    private var repeat = RepeatMode.ALL
    private var volume = 1.0
    private var ended = false
    /** ExoPlayer's playWhenReady: whether item changes keep playing. */
    private var playWhenReady = false

    init {
        FxToolkit.ensureStarted()
        scope.launch {
            while (isActive) {
                delay(pollIntervalMs)
                FxToolkit.runOnFx { pollPosition() }
            }
        }
    }

    /** Replaces the queue and starts playing [startIndex] from 0. */
    fun playQueue(tracks: List<Track>, startIndex: Int = 0) = FxToolkit.runOnFx {
        this.tracks = tracks.toList()
        navigator.setQueue(this.tracks.size, startIndex)
        _state.update { it.copy(queue = this.tracks) }
        if (this.tracks.isEmpty()) {
            release(); index = -1; publish()
        } else {
            playWhenReady = true
            load(startIndex.coerceIn(0, this.tracks.lastIndex), autoplay = true)
        }
    }

    fun play() = FxToolkit.runOnFx {
        val p = player ?: return@runOnFx
        playWhenReady = true
        if (ended) {
            ended = false
            p.seek(Duration.ZERO)
            _transitions.tryEmit(tracks[index])
        }
        p.play()
    }

    fun pause() = FxToolkit.runOnFx {
        playWhenReady = false
        player?.pause()
    }

    fun toggle() = FxToolkit.runOnFx {
        if (playWhenReady && !ended) pause() else play()
    }

    fun next() = FxToolkit.runOnFx {
        if (index < 0) return@runOnFx
        navigator.nextIndex(index, repeat)?.let { load(it, autoplay = playWhenReady) }
    }

    fun previous() = FxToolkit.runOnFx {
        val p = player ?: return@runOnFx
        val pos = p.currentTime.toMillis().toLong()
        val prev = navigator.previousIndex(index, repeat)
        if (prev != null && QueueNavigator.previousGoesBack(pos, hasPrevious = true)) {
            load(prev, autoplay = playWhenReady)
        } else {
            seekInternal(0)
        }
    }

    /** Jumps to queue item [i] and plays it. */
    fun skipTo(i: Int) = FxToolkit.runOnFx {
        if (i in tracks.indices) {
            playWhenReady = true
            load(i, autoplay = true)
        }
    }

    fun seekTo(ms: Long) = FxToolkit.runOnFx { seekInternal(ms) }

    fun setRepeatMode(mode: RepeatMode) = FxToolkit.runOnFx {
        repeat = mode
        publish()
    }

    /** OFF → ALL → ONE → OFF (Android toggle order). */
    fun cycleRepeatMode() = FxToolkit.runOnFx {
        repeat = repeat.next()
        publish()
    }

    fun setShuffle(enabled: Boolean) = FxToolkit.runOnFx {
        navigator.shuffleEnabled = enabled
        publish()
    }

    fun toggleShuffle() = FxToolkit.runOnFx {
        navigator.shuffleEnabled = !navigator.shuffleEnabled
        publish()
    }

    /** 0.0 .. 1.0 */
    fun setVolume(v: Double) = FxToolkit.runOnFx {
        volume = v.coerceIn(0.0, 1.0)
        player?.volume = volume
        publish()
    }

    fun stop() = FxToolkit.runOnFx {
        playWhenReady = false
        player?.stop()
        publish()
    }

    /** Disposes the media player and the poller. The instance must not be used afterwards. */
    fun shutdown() {
        scope.cancel()
        FxToolkit.runOnFx { release() }
    }

    // ---------------------------------------------------------------------------------------------

    private fun isPlayingNow() = player?.status == MediaPlayer.Status.PLAYING

    private fun release() {
        player?.let {
            it.onEndOfMedia = null
            it.onError = null
            it.onReady = null
            runCatching { it.stop() }
            runCatching { it.dispose() }
        }
        player = null
    }

    private fun load(i: Int, autoplay: Boolean) {
        release()
        index = i
        ended = false
        val track = tracks[i]
        _state.update {
            it.copy(currentIndex = i, currentTrack = track, positionMs = 0, durationMs = track.durationMs, ended = false)
        }
        _transitions.tryEmit(track)

        val mp = try {
            MediaPlayer(Media(File(track.path).toURI().toString()))
        } catch (e: Exception) {
            _state.update { it.copy(isPlaying = false, error = "Cannot play ${track.path}: ${e.message}") }
            publish()
            return
        }
        player = mp
        mp.volume = volume
        mp.onReady = Runnable {
            if (player !== mp) return@Runnable
            val d = mp.media.duration
            if (d != null && !d.isUnknown && !d.isIndefinite) {
                _state.update { it.copy(durationMs = d.toMillis().toLong().coerceAtLeast(0)) }
            }
        }
        mp.onEndOfMedia = Runnable { if (player === mp) handleEndOfMedia() }
        mp.onError = Runnable {
            if (player === mp) {
                _state.update { it.copy(isPlaying = false, error = mp.error?.message ?: "Playback error") }
            }
        }
        mp.statusProperty().addListener { _, _, status ->
            if (player === mp) {
                _state.update { it.copy(isPlaying = status == MediaPlayer.Status.PLAYING) }
            }
        }
        _state.update { it.copy(error = null) }
        if (autoplay) mp.play()
        publish()
    }

    private fun handleEndOfMedia() {
        val nextIdx = navigator.autoAdvanceIndex(index, repeat)
        if (stopAfterCurrentTrack) {
            stopAfterCurrentTrack = false
            playWhenReady = false
            when {
                nextIdx == null -> {
                    ended = true
                    player?.pause()
                    _state.update { it.copy(isPlaying = false, ended = true, positionMs = it.durationMs) }
                }
                nextIdx == index -> {
                    player?.pause()
                    player?.seek(Duration.ZERO)
                    _state.update { it.copy(isPlaying = false, positionMs = 0) }
                    _transitions.tryEmit(tracks[index])
                }
                else -> load(nextIdx, autoplay = false)
            }
            _stoppedAtTrackEnd.tryEmit(Unit)
            return
        }
        when {
            nextIdx == null -> {
                // Repeat OFF after the last item: ENDED, not playing
                ended = true
                player?.pause()
                _state.update { it.copy(isPlaying = false, ended = true, positionMs = it.durationMs) }
            }
            nextIdx == index -> {
                // ONE (or ALL with a single item): restart the same media, still a transition
                player?.seek(Duration.ZERO)
                player?.play()
                _state.update { it.copy(positionMs = 0) }
                _transitions.tryEmit(tracks[index])
            }
            else -> load(nextIdx, autoplay = true)
        }
    }

    private fun seekInternal(ms: Long) {
        val p = player ?: return
        if (ended) ended = false
        p.seek(Duration.millis(ms.coerceAtLeast(0).toDouble()))
        _state.update { it.copy(positionMs = ms.coerceAtLeast(0), ended = false) }
    }

    private fun pollPosition() {
        val p = player ?: return
        if (ended) return
        val pos = p.currentTime?.toMillis()?.toLong() ?: return
        val d = p.media?.duration
        _state.update {
            it.copy(
                positionMs = pos.coerceAtLeast(0),
                durationMs = if (d != null && !d.isUnknown && !d.isIndefinite) d.toMillis().toLong() else it.durationMs,
                isPlaying = p.status == MediaPlayer.Status.PLAYING,
            )
        }
    }

    private fun publish() {
        _state.update {
            it.copy(
                currentIndex = index,
                currentTrack = tracks.getOrNull(index),
                repeatMode = repeat,
                shuffleEnabled = navigator.shuffleEnabled,
                volume = volume,
                queue = tracks,
                ended = ended,
                isPlaying = isPlayingNow(),
            )
        }
    }
}
