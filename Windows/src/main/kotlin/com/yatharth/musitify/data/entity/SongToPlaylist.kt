package com.yatharth.musitify.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Desktop deviation from Android v9: adds a UNIQUE index on (songId, playListId)
 * so the same song cannot appear twice in a playlist (Android allows duplicates,
 * and every sync pull re-inserts relations). The composite index also covers the
 * songId FK; a separate playListId index covers the other FK and per-playlist queries.
 * Consequence: the desktop schema identity hash differs from Android's, so an
 * Android DB file cannot be opened as-is (fresh desktop DB only).
 */
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Song::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playListId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["songId", "playListId"], unique = true),
        Index(value = ["playListId"])
    ]
)
data class SongToPlaylist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val songId: Int,
    val playListId: Int,
    val position: Int,
    val addedOn: Long,
    var song_uuid: UUID? = null,
    var playlist_uuid: UUID? = null
)
