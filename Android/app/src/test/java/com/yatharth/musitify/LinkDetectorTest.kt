package com.yatharth.musitify

import com.yatharth.musitify.util.LinkDetector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkDetectorTest {

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
}
