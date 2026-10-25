package com.yatharth.musitify.util

import com.yatharth.musitify.data.entity.Song

/**
 * Display-only helpers. Stored names are normalized and often start with the
 * artist ("the weeknd life of the party"). The artist is shown on its own
 * line, so repeating it in the title is noise. The stored name is untouched.
 */
object SongDisplay {

    private val whitespace = Regex("\\s+")
    private val punctuation = Regex("[^a-z0-9\\s]")

    private fun collapse(s: String) = s.replace(whitespace, " ").trim()

    // Stored names drop punctuation ("AC/DC" -> "acdc"); try that and the
    // spaced form ("ac dc") so either way matches.
    private fun artistForms(artist: String): Set<String> {
        val lower = artist.lowercase()
        return setOf(
            collapse(lower.replace(punctuation, "")),
            collapse(lower.replace(punctuation, " "))
        ).filter { it.isNotEmpty() && it != "unknown" }.toSet()
    }

    /** "the weeknd life of the party" by "The Weeknd" -> "life of the party". */
    fun title(name: String, artist: String?): String {
        val trimmed = name.trim()
        val lower = trimmed.lowercase()
        for (a in artistForms(artist.orEmpty())) {
            val prefix = "$a "
            if (lower.startsWith(prefix) && trimmed.length > prefix.length) {
                return trimmed.substring(prefix.length).trim()
            }
        }
        return trimmed
    }

    fun title(song: Song): String = title(song.name, song.artist)

    /** Artist line, or null when unknown. */
    fun artist(song: Song): String? =
        song.artist.trim().takeIf { it.isNotEmpty() && !it.equals("unknown", ignoreCase = true) }
}
