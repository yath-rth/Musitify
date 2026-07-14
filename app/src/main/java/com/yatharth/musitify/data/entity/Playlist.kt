package com.yatharth.musitify.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val timeHeard: Int,
    val createdAt: Int
)
