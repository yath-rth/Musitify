package com.yatharth.musitify.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

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
