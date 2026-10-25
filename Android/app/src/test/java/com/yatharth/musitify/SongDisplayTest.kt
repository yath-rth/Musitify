package com.yatharth.musitify

import com.yatharth.musitify.util.SongDisplay
import org.junit.Assert.assertEquals
import org.junit.Test

class SongDisplayTest {

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
