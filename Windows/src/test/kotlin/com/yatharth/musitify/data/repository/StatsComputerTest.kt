package com.yatharth.musitify.data.repository

import com.yatharth.musitify.data.entity.ListeningSession
import java.util.Calendar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Port of Android's StatsComputerTest: rolling 7/30-day windows and streak rules. */
class StatsComputerTest {

    private val minute = 60_000L

    /** 12:00 local time, [daysAgo] days before [now]. */
    private fun noon(now: Long, daysAgo: Int): Long = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_MONTH, -daysAgo)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun session(at: Long, minutes: Long = 30) = ListeningSession(
        songId = 1,
        songName = "song",
        artistName = "artist",
        songLength = 3 * minute,
        startTimeMs = at,
        durationMs = minutes * minute
    )

    /** A Sunday evening, i.e. the first day of the week in many locales. */
    private val sundayEvening: Long = Calendar.getInstance().apply {
        set(2026, Calendar.OCTOBER, 25, 22, 30, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun streakSurvivesUntilTodayIsOver() {
        // Listened every day for the last 5 days, not yet today.
        val sessions = (1..5).map { session(noon(sundayEvening, it)) }
        assertEquals(5, StatsComputer.compute(sessions, sundayEvening).currentStreak)
    }

    @Test
    fun streakIncludesToday() {
        val sessions = (0..2).map { session(noon(sundayEvening, it)) }
        assertEquals(3, StatsComputer.compute(sessions, sundayEvening).currentStreak)
    }

    @Test
    fun streakBreaksAfterAMissedDay() {
        // Yesterday and 3 days ago; the day in between was missed.
        val sessions = listOf(session(noon(sundayEvening, 1)), session(noon(sundayEvening, 3)))
        assertEquals(1, StatsComputer.compute(sessions, sundayEvening).currentStreak)
    }

    @Test
    fun longestStreakNeedsConsecutiveDays() {
        // 3 days in a row, a gap, then 2 in a row: longest is 3, not 5.
        val days = listOf(10, 9, 8, 5, 4)
        val sessions = days.map { session(noon(sundayEvening, it)) }
        assertEquals(3, StatsComputer.compute(sessions, sundayEvening).longestStreak)
    }

    @Test
    fun lastSevenDaysDoesNotResetAtTheStartOfTheWeek() {
        // 30 min on each of the last 6 days, nothing today (a Sunday).
        val sessions = (1..6).map { session(noon(sundayEvening, it)) }
        val stats = StatsComputer.compute(sessions, sundayEvening)
        assertEquals(6 * 30 * minute, stats.thisWeekListenMs)
        assertEquals(0L, stats.todayListenMs)
    }

    @Test
    fun lastSevenDaysExcludesOlderListening() {
        val sessions = listOf(session(noon(sundayEvening, 6)), session(noon(sundayEvening, 7)))
        assertEquals(30 * minute, StatsComputer.compute(sessions, sundayEvening).thisWeekListenMs)
    }

    @Test
    fun lastThirtyDaysDoesNotResetOnTheFirst() {
        val firstOfMonth = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 1, 9, 0, 0)
        }.timeInMillis
        val sessions = (1..10).map { session(noon(firstOfMonth, it)) }
        val stats = StatsComputer.compute(sessions, firstOfMonth)
        assertEquals(10 * 30 * minute, stats.thisMonthListenMs)
        assertTrue(stats.topSongsThisMonth.isNotEmpty())
    }
}
