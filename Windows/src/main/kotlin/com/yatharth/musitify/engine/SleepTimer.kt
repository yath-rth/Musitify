package com.yatharth.musitify.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SleepTimerDuration(val minutes: Int, val label: String) {
    FIVE_MINUTES(5, "5 minutes"),
    TEN_MINUTES(10, "10 minutes"),
    FIFTEEN_MINUTES(15, "15 minutes"),
    THIRTY_MINUTES(30, "30 minutes"),
    FORTY_FIVE_MINUTES(45, "45 minutes"),
    SIXTY_MINUTES(60, "60 minutes");

    fun toMillis(): Long = minutes * 60 * 1000L
}

data class SleepTimerState(
    val isActive: Boolean = false,
    val duration: SleepTimerDuration? = null,
    val remainingMs: Long = 0L,
    // Stop when the current song finishes instead of after a fixed time.
    val endOfSong: Boolean = false
)

/**
 * Port of Android `SleepTimerManager`: one timer at a time, wall-clock based, state refreshed
 * every [tickMs] (1 s); on expiry the state resets and `onComplete` runs (wire it to
 * `AudioPlayer.pause()` — immediate pause, as Android). Counts down while paused too.
 *
 * Fix vs Android: [extendTimer] moves the end time, so the extension actually takes effect.
 */
class SleepTimer(
    private val tickMs: Long = 1000,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null

    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private var onTimerComplete: (() -> Unit)? = null

    @Volatile
    private var endTime = 0L

    fun startTimer(duration: SleepTimerDuration, onComplete: () -> Unit) =
        start(duration, duration.toMillis(), onComplete)

    /** Same as [startTimer] with an arbitrary length (tests / custom durations). */
    @Synchronized
    fun start(duration: SleepTimerDuration?, totalMs: Long, onComplete: () -> Unit) {
        cancelTimer()
        onTimerComplete = onComplete
        endTime = clock() + totalMs
        _state.value = SleepTimerState(isActive = true, duration = duration, remainingMs = totalMs)

        timerJob = scope.launch {
            while (true) {
                val remaining = (endTime - clock()).coerceAtLeast(0)
                _state.value = _state.value.copy(remainingMs = remaining)
                if (remaining <= 0) {
                    val cb = onTimerComplete
                    _state.value = SleepTimerState()
                    onTimerComplete = null
                    timerJob = null
                    cb?.invoke()
                    break
                }
                delay(minOf(tickMs, remaining))
            }
        }
    }

    /** Pause when the current song ends. The player reports the end; see MediaControllerRepository. */
    @Synchronized
    fun startEndOfSong() {
        cancelTimer()
        _state.value = SleepTimerState(isActive = true, endOfSong = true)
    }

    @Synchronized
    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _state.value = SleepTimerState()
        onTimerComplete = null
    }

    @Synchronized
    fun extendTimer(additionalMinutes: Int) = extendBy(additionalMinutes * 60 * 1000L)

    @Synchronized
    fun extendBy(additionalMs: Long) {
        val current = _state.value
        if (!current.isActive) return
        endTime += additionalMs
        _state.value = current.copy(remainingMs = (endTime - clock()).coerceAtLeast(0))
    }
}
