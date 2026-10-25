package com.yatharth.musitify

import com.yatharth.musitify.util.Formatter
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatterTest {

    @Test
    fun countUsesSingularOnlyForOne() {
        assertEquals("0 songs", Formatter.count(0, "song"))
        assertEquals("1 song", Formatter.count(1, "song"))
        assertEquals("2 songs", Formatter.count(2, "song"))
        assertEquals("1 play", Formatter.count(1L, "play"))
    }

    @Test
    fun listenTimeSaysOneMinNotOneMins() {
        assertEquals("1 min", Formatter.formatListenTime(60_000))
        assertEquals("3 mins", Formatter.formatListenTime(3 * 60_000))
        assertEquals("1 hr 1 min", Formatter.formatListenTime(61 * 60_000))
        assertEquals("2 hrs", Formatter.formatListenTime(120 * 60_000))
    }

    @Test
    fun listenTimeUnderAMinuteIsNotZero() {
        assertEquals("0 mins", Formatter.formatListenTime(0))
        assertEquals("<1 min", Formatter.formatListenTime(30_000))
    }

    @Test
    fun playlistLengthUsesSingularMinute() {
        assertEquals("1 min", Formatter.formatTimeToMinutes(60_000))
        assertEquals("49 mins", Formatter.formatTimeToMinutes(49 * 60_000))
        assertEquals("1 hr 1 min", Formatter.formatTimeToMinutes(61 * 60_000))
    }
}
