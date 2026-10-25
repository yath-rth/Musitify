package com.yatharth.musitify.util

import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.engine.CoverCollage

/** Desktop counterpart of Android `util.SongMedia` (collage lives in the engine [CoverCollage]). */
object SongMedia {
    /** Android: `round(timeHeard / length)`, or 0 when length is 0. */
    fun playCountOf(song: Song): Int = CoverCollage.playCountOf(song.timeHeard.toLong(), song.length)
}
