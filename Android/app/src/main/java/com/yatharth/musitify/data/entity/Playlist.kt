package com.yatharth.musitify.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    indices = [Index(value = ["name"], unique = true)]
)
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val timeHeard: Int = 0,
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