package com.yatharth.musitify.engine

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class ArtistTest {
    private fun info(json: String) = Json.parseToJsonElement(json) as JsonObject

    @Test
    fun cleansChannelNames() {
        val cases = mapOf(
            "Daft Punk - Topic" to "Daft Punk",
            "DaftPunkVEVO" to "Daft Punk",
            "TaylorSwiftVEVO" to "Taylor Swift",
            "ACDCVEVO" to "ACDC",
            "Coldplay Official" to "Coldplay",
            "Official Coldplay" to "Coldplay",
            "Linkin Park (Official)" to "Linkin Park",
            "Imagine Dragons - Official Artist Channel" to "Imagine Dragons",
            "Unofficial Band" to "Unofficial Band",
            "Hot Topic" to "Hot Topic",
            "Vevo" to "Vevo",
            "Official" to "Official",
            "Kendrick Lamar" to "Kendrick Lamar",
            "  The Weeknd  " to "The Weeknd",
        )
        cases.forEach { (input, expected) -> assertEquals(expected, Downloader.cleanArtist(input), input) }
    }

    @Test
    fun fallbackChain() {
        assertEquals("Daft Punk", Downloader.artistOf(info("""{"artist":null,"artists":null,"uploader":"DaftPunkVEVO"}""")))
        assertEquals("A, B", Downloader.artistOf(info("""{"artists":["A","B"]}""")))
        assertEquals("Unknown Artist", Downloader.artistOf(info("""{"artist":"","artists":[],"uploader":""}""")))
        assertEquals("Unknown Artist", Downloader.artistOf(info("{}")))
        assertEquals("Queen", Downloader.artistOf(info("""{"artist":"Queen - Topic"}""")))
    }
}
