package com.yatharth.musitify.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val length: Int,
    val timeHeard: Int,
    val downloadedAt: Long
)