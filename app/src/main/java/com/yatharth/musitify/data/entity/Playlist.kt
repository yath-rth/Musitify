package com.yatharth.musitify.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val timeHeard: Int,
    val artPath: String? = null,
    val createdAt: Long,
    var uuid: UUID? = null
)

data class PlaylistInfo(
    @Embedded
    val playlist: Playlist,
    val numOfSongs: Int,
    val playlistLength: Long
)