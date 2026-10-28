package com.yatharth.musitify.data.repository

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class YouTubeSearchParseTest {

    private val repo = YouTubeSearchRepository(ytDlp = { File("yt-dlp.exe") })

    @Test
    fun parsesFlatSearchEntries() {
        val stdout = """
            {"_type":"playlist","entries":[
              {"id":"abc123","title":"Blinding Lights","channel":"The Weeknd","uploader":"TheWeekndVEVO","duration":200.0},
              {"id":"def456","title":"","uploader":"Some Channel - Topic"},
              {"title":"no id, skipped"}
            ]}
        """.trimIndent().replace("\n", "")

        val results = repo.parseSearch(stdout)

        assertEquals(2, results.size)
        val first = results[0]
        assertEquals("abc123", first.id)
        assertEquals("https://www.youtube.com/watch?v=abc123", first.url)
        assertEquals("Blinding Lights", first.title)
        assertEquals("The Weeknd", first.artist)
        assertEquals(200L, first.durationSeconds)
        assertTrue(first.thumbnailUrl.contains("abc123"))

        assertEquals("Unknown title", results[1].title)
        assertEquals("Some Channel", results[1].artist)
        assertNull(results[1].durationSeconds)
    }

    @Test
    fun noJsonMeansNoResults() {
        assertTrue(repo.parseSearch("").isEmpty())
    }
}
