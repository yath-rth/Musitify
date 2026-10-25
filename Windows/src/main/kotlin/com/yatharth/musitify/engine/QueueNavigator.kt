package com.yatharth.musitify.engine

import kotlin.random.Random

/** Same integer values as Media3 `Player.REPEAT_MODE_*` (0 OFF, 1 ONE, 2 ALL). */
enum class RepeatMode(val value: Int) {
    OFF(0), ONE(1), ALL(2);

    /** Android toggle order: OFF → ALL → ONE → OFF. */
    fun next(): RepeatMode = when (this) {
        OFF -> ALL
        ALL -> ONE
        ONE -> OFF
    }

    companion object {
        fun fromValue(v: Int) = entries.firstOrNull { it.value == v } ?: OFF
    }
}

/**
 * Pure queue-navigation rules emulating ExoPlayer defaults:
 *  - Navigation (next/previous buttons) treats ONE as OFF; ALL wraps around.
 *  - Auto-advance at end of media: ONE repeats the item, ALL wraps, OFF stops after the last item.
 *  - Shuffle uses a permutation generated when the queue is set (toggling doesn't reshuffle).
 */
class QueueNavigator(private val random: Random = Random.Default) {
    var size: Int = 0
        private set
    var shuffleEnabled: Boolean = false
    private var shuffleOrder: List<Int> = emptyList()

    fun setQueue(size: Int, startIndex: Int = 0) {
        this.size = size
        // Like DefaultShuffleOrder-with-start: the start item first, the rest shuffled.
        val rest = (0 until size).filter { it != startIndex }.shuffled(random)
        shuffleOrder = if (startIndex in 0 until size) listOf(startIndex) + rest else rest
    }

    fun order(): List<Int> = if (shuffleEnabled) shuffleOrder else (0 until size).toList()

    private fun neighbour(current: Int, step: Int, wrap: Boolean): Int? {
        if (size == 0) return null
        val order = order()
        val pos = order.indexOf(current)
        if (pos < 0) return null
        val target = pos + step
        return when {
            target in order.indices -> order[target]
            wrap -> order[(target + order.size) % order.size]
            else -> null
        }
    }

    /** Next item for the "next" button (ONE behaves like OFF). */
    fun nextIndex(current: Int, repeat: RepeatMode): Int? = neighbour(current, 1, repeat == RepeatMode.ALL)

    /** Previous item for the "previous" button (ONE behaves like OFF). */
    fun previousIndex(current: Int, repeat: RepeatMode): Int? = neighbour(current, -1, repeat == RepeatMode.ALL)

    /** Item to play when [current] finishes, or null when playback should end. */
    fun autoAdvanceIndex(current: Int, repeat: RepeatMode): Int? = when (repeat) {
        RepeatMode.ONE -> current.takeIf { it in 0 until size }
        RepeatMode.ALL -> neighbour(current, 1, wrap = true)
        RepeatMode.OFF -> neighbour(current, 1, wrap = false)
    }

    companion object {
        /** Media3 default `maxSeekToPreviousPositionMs`. */
        const val MAX_SEEK_TO_PREVIOUS_MS = 3_000L

        /** true → go to previous item; false → restart current item. */
        fun previousGoesBack(positionMs: Long, hasPrevious: Boolean) =
            hasPrevious && positionMs <= MAX_SEEK_TO_PREVIOUS_MS
    }
}
