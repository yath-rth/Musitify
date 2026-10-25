package com.yatharth.musitify.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class ListenTrackerTest {
    private val events = mutableListOf<Pair<Long, Long>>()
    private val tracker = ListenTracker { id, ms -> events += id to ms }

    @Test
    fun countsDeltasCappedAt15s() {
        tracker.onTrackChanged(1)
        tracker.onTick(7_000)
        tracker.onTick(60_000) // forward seek → capped
        tracker.onTick(10_000) // backward seek → 0, no event
        tracker.onTick(17_000)
        assertEquals(listOf(1L to 7_000L, 1L to 15_000L, 1L to 7_000L), events)
    }

    @Test
    fun transitionResetsLastPosition() {
        tracker.onTrackChanged(1)
        tracker.onTick(7_000)
        tracker.onTrackChanged(2)
        tracker.onTick(5_000)
        // ONE-repeat restart of same id also resets
        tracker.onTrackChanged(2)
        tracker.onTick(3_000)
        assertEquals(listOf(1L to 7_000L, 2L to 5_000L, 2L to 3_000L), events)
    }

    @Test
    fun noTrackNoEventsAndFlushIsNoOpBetweenTicks() {
        tracker.onTick(7_000)
        assertEquals(emptyList(), events)
        tracker.onTrackChanged(3)
        tracker.flush()
        tracker.onTick(0)
        assertEquals(emptyList(), events)
    }

    @Test
    fun lastPositionSurvivesPause() {
        tracker.onTrackChanged(1)
        tracker.onTick(7_000)
        tracker.flush() // pause
        tracker.onTick(12_000) // resumed; time before pause still counted
        assertEquals(listOf(1L to 7_000L, 1L to 5_000L), events)
    }
}
