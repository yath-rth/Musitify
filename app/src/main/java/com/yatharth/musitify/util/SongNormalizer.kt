package com.yatharth.musitify.util

import java.text.Normalizer
import java.util.Locale
import java.util.regex.Pattern

object SongNormalizer {
    private val COSMETIC_NOISE = mutableListOf<Pattern>(
        Pattern.compile("\\(official.*?\\)|\\[official.*?]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\(lyric.*?\\)|\\[lyric.*?]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\(audio\\)|\\[audio]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\(hd\\)|\\[hd]|\\(hq\\)|\\[hq]", Pattern.CASE_INSENSITIVE),
        Pattern.compile(
            "\\(explicit\\)|\\[explicit]",
            Pattern.CASE_INSENSITIVE
        ) // explicit tag ≠ different song
    )

    private val VERSION_MARKER: Pattern = Pattern.compile(
        "\\((remaster(ed)?( \\d{4})?|live|acoustic|deluxe|radio edit|extended|instrumental|clean|demo)\\)",
        Pattern.CASE_INSENSITIVE
    )

    fun normalize(rawTitle: String): NormalizedSong {
        var s = Normalizer.normalize(rawTitle, Normalizer.Form.NFKD)
            .replace("\\p{M}".toRegex(), "")
            .lowercase(Locale.getDefault())

        // pull out feat./ft. into nothing (doesn't define a "version")
        s = s.replace("\\(feat\\..*?\\)|\\[feat\\..*?\\]|\\(ft\\..*?\\)".toRegex(), "")

        // extract version tag BEFORE stripping it
        var versionTag = ""
        val vm = VERSION_MARKER.matcher(s)
        if (vm.find()) {
            versionTag = vm.group(1).replace("\\s+\\d{4}".toRegex(), "")
                .trim { it <= ' ' }  // "remastered 2009" -> "remastered"
        }
        s = VERSION_MARKER.matcher(s).replaceAll("")

        // strip true cosmetic noise
        for (p in COSMETIC_NOISE) s = p.matcher(s).replaceAll("")

        s = s.replace("[^a-z0-9\\s]".toRegex(), "").replace("\\s+".toRegex(), " ")
            .trim { it <= ' ' }

        s = s.replace("nana", "")

        return NormalizedSong(s, versionTag)
    }

    class NormalizedSong(// core title, version-agnostic
        val normalizedTitle: String, // "", "live", "remastered", "acoustic", etc.
        val versionTag: String
    )
}