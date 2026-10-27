package com.yatharth.musitify.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Ports of Android's FormatterTest, LinkDetectorTest and SongDisplayTest. */
class DisplayHelpersTest {

    // ---- Formatter ----

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

    // ---- LinkDetector ----

    @Test
    fun playlistPagesArePlaylists() {
        assertTrue(LinkDetector.isPlaylist("https://www.youtube.com/playlist?list=OLAK5uy_abc"))
        assertTrue(LinkDetector.isPlaylist("https://music.youtube.com/playlist?list=PL123"))
        assertTrue(LinkDetector.isPlaylist("youtube.com/watch?list=PL123"))
    }

    @Test
    fun videosAreSingleSongs() {
        assertFalse(LinkDetector.isPlaylist("https://www.youtube.com/watch?v=_bSXY75R3Wc"))
        assertFalse(LinkDetector.isPlaylist("https://youtu.be/_bSXY75R3Wc"))
        assertFalse(LinkDetector.isPlaylist("https://youtube.com/shorts/abc"))
    }

    @Test
    fun videoOpenedFromAPlaylistIsTheSong() {
        assertFalse(LinkDetector.isPlaylist("https://www.youtube.com/watch?v=abc&list=PL123&index=3"))
        assertFalse(LinkDetector.isPlaylist("https://youtu.be/abc?list=PL123"))
    }

    @Test
    fun blankIsNotAPlaylist() {
        assertFalse(LinkDetector.isPlaylist("   "))
    }

    // ---- SongDisplay ----

    @Test
    fun stripsLeadingArtist() {
        assertEquals("life of the party", SongDisplay.title("the weeknd life of the party", "The Weeknd"))
        assertEquals("the zone ft drake", SongDisplay.title("the weeknd the zone ft drake", "The Weeknd"))
    }

    @Test
    fun keepsTitleWhenArtistIsNotAPrefix() {
        assertEquals("blinding lights", SongDisplay.title("blinding lights", "The Weeknd"))
        assertEquals("weekend vibes", SongDisplay.title("weekend vibes", "Weeknd"))
    }

    @Test
    fun neverReturnsEmpty() {
        assertEquals("the weeknd", SongDisplay.title("the weeknd", "The Weeknd"))
    }

    @Test
    fun ignoresUnknownArtist() {
        assertEquals("unknown song", SongDisplay.title("unknown song", "unknown"))
        assertEquals("song", SongDisplay.title("song", null))
    }

    @Test
    fun matchesArtistWithPunctuation() {
        assertEquals("song two", SongDisplay.title("acdc song two", "AC/DC"))
        assertEquals("song two", SongDisplay.title("ac dc song two", "AC/DC"))
    }
}
