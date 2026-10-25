package com.yatharth.musitify.util

/**
 * Works out what a pasted link points at, so the Add songs dialog doesn't need
 * a playlist/video switch.
 */
object LinkDetector {

    private val listParam = Regex("[?&]list=")
    private val videoParam = Regex("[?&]v=")

    /**
     * True for playlist pages ("/playlist?list=..."), or links that have a
     * list but no video. A video opened from inside a playlist
     * ("watch?v=X&list=Y") counts as a single song: that's the one being shared.
     */
    fun isPlaylist(url: String): Boolean {
        val u = url.trim().lowercase()
        if (u.isEmpty()) return false
        if ("/playlist" in u) return true
        val pointsAtVideo = videoParam.containsMatchIn(u) || "youtu.be/" in u || "/shorts/" in u
        return listParam.containsMatchIn(u) && !pointsAtVideo
    }
}
