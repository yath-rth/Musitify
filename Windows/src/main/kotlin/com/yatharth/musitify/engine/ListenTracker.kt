package com.yatharth.musitify.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

/**
 * Listen-time accounting, port of `MediaControllerRepository` tracking rules:
 *  - While playing, every [tickMs] (7 s): `delta = (position - lastPosition).coerceIn(0, capMs /*15 s*/)`,
 *    `lastPosition = position`, then flush → one `(trackId, deltaMs)` event when delta > 0.
 *  - Backward seeks count 0; forward seeks count at most 15 s per tick.
 *  - Pause stops ticking (and flushes); `lastPosition` survives the pause.
 *  - A track transition (including ONE-repeat restarts) flushes and resets `lastPosition = 0`.
 *
 * Persistence (song/playlist timeHeard, ListeningSession rows) is done by [onListen].
 * The pure methods ([onTrackChanged], [onTick], [flush]) are not thread-safe on their own;
 * [bind] serializes its calls to them.
 */
class ListenTracker(
    private val tickMs: Long = 7_000,
    private val capMs: Long = 15_000,
    private val onListen: (trackId: Long, deltaMs: Long) -> Unit,
) {
    var trackId: Long? = null
        private set
    private var accumulatedMs = 0L
    private var lastPositionMs = 0L

    fun onTrackChanged(newTrackId: Long?) {
        flush()
        trackId = newTrackId
        accumulatedMs = 0
        lastPositionMs = 0
    }

    fun onTick(positionMs: Long) {
        val delta = (positionMs - lastPositionMs).coerceIn(0, capMs)
        accumulatedMs += delta
        lastPositionMs = positionMs
        flush()
    }

    fun flush() {
        val id = trackId ?: return
        if (accumulatedMs <= 0) return
        val ms = accumulatedMs
        accumulatedMs = 0
        onListen(id, ms)
    }

    /**
     * Drives this tracker from an [AudioPlayer] (single coroutine; call once).
     * Returns the job so it can be cancelled.
     */
    fun bind(scope: CoroutineScope, player: AudioPlayer): Job = scope.launch {
        var ticker: Job? = null
        launch {
            player.transitions.collect { track -> onTrackChangedSafe(track.id) }
        }
        player.state.distinctUntilChangedBy { it.isPlaying }.collect { s ->
            ticker?.cancel()
            ticker = null
            if (s.isPlaying) {
                if (trackId == null) onTrackChangedSafe(s.currentTrack?.id)
                ticker = launch {
                    while (true) {
                        delay(tickMs)
                        tick(player.state.value.positionMs)
                    }
                }
            } else {
                flushSafe()
            }
        }
    }

    private val lock = Any()
    private fun onTrackChangedSafe(id: Long?) = synchronized(lock) { onTrackChanged(id) }
    private fun tick(pos: Long) = synchronized(lock) { onTick(pos) }
    private fun flushSafe() = synchronized(lock) { flush() }
}
