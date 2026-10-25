package com.yatharth.musitify.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Snapshot of a chunk of actual listening time. Written every ~7s while
 * audio plays (same cadence as the existing timeHeard tracking) plus on
 * track transitions. Names / art are denormalized so stats survive a song
 * or playlist being deleted from the library.
 */
@Entity(
    tableName = "ListeningSessions",
    indices = [
        Index("songId"),
        Index("playlistId"),
        Index("startTimeMs")
    ]
)
data class ListeningSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val songId: Int? = null,
    val playlistId: Int? = null,
    val songName: String? = null,
    val artistName: String? = null,
    val playlistName: String? = null,
    val songArtPath: String? = null,
    val songLength: Long = 0,
    val startTimeMs: Long,
    val durationMs: Long
)