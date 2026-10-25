package com.yatharth.musitify.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class SongNormalizerTest {
    private fun n(s: String) = SongNormalizer.normalize(s)

    @Test
    fun stripsOfficialVideoNoise() {
        assertEquals("song title", n("Song Title (Official Video)").normalizedTitle)
        assertEquals("song title", n("Song Title [Official Music Video]").normalizedTitle)
        assertEquals("song", n("Song (Lyrics)").normalizedTitle)
        assertEquals("song", n("Song (Audio)").normalizedTitle)
        assertEquals("song", n("Song (Explicit)").normalizedTitle)
    }

    @Test
    fun extractsVersionTagAndDropsYear() {
        val r = n("Bohemian Rhapsody (Remastered 2011)")
        assertEquals("bohemian rhapsody", r.normalizedTitle)
        assertEquals("remastered", r.versionTag)
        assertEquals("live", n("Song (Live)").versionTag)
        assertEquals("", n("Song").versionTag)
    }

    @Test
    fun removesFeatAndFoldsAccents() {
        assertEquals("cafe del mar", n("Café del Mar (feat. Someone) [HD]").normalizedTitle)
        assertEquals("track", n("Track (ft. Other)").normalizedTitle)
        // "[ft. x]" is NOT matched by the Android regex: only punctuation is dropped
        assertEquals("track ft x", n("Track [ft. X]").normalizedTitle)
    }

    @Test
    fun punctuationWhitespaceAndQuirks() {
        assertEquals("hello world", n("  Hello -   World!! ").normalizedTitle)
        assertEquals("", n("日本語").normalizedTitle)
        // Android quirk preserved: literal "nana" removal
        assertEquals("ba split", n("Banana Split").normalizedTitle)
    }
}
