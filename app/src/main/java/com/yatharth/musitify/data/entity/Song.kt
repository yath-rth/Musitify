package com.yatharth.musitify.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [Index(value = ["name"], unique = true)]
)
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val length: Int,
    val filePath: String,
    val timeHeard: Int,
    val downloadedAt: Long
)