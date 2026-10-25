package com.yatharth.musitify.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

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

@Singleton
class SleepTimerManager @Inject constructor() {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null

    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private var onTimerComplete: (() -> Unit)? = null

    fun startTimer(duration: SleepTimerDuration, onComplete: () -> Unit) {
        cancelTimer()
        onTimerComplete = onComplete

        val totalMs = duration.toMillis()
        _state.value = SleepTimerState(
            isActive = true,
            duration = duration,
            remainingMs = totalMs
        )

        timerJob = scope.launch {
            val startTime = System.currentTimeMillis()
            val endTime = startTime + totalMs

            while (true) {
                val currentTime = System.currentTimeMillis()
                val remaining = (endTime - currentTime).coerceAtLeast(0)

                _state.value = _state.value.copy(remainingMs = remaining)

                if (remaining <= 0) {
                    _state.value = SleepTimerState()
                    onTimerComplete?.invoke()
                    break
                }

                delay(1000) // Update every second
            }
        }
    }

    /** Pause when the current song ends. The player reports the end; see
     *  MediaControllerRepository. */
    fun startEndOfSong() {
        cancelTimer()
        _state.value = SleepTimerState(isActive = true, endOfSong = true)
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _state.value = SleepTimerState()
        onTimerComplete = null
    }

    fun extendTimer(additionalMinutes: Int) {
        val currentState = _state.value
        if (currentState.isActive && currentState.duration != null) {
            val newRemainingMs = currentState.remainingMs + (additionalMinutes * 60 * 1000L)
            _state.value = currentState.copy(remainingMs = newRemainingMs)
        }
    }
}
