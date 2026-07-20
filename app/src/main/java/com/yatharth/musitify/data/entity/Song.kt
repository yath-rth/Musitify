package com.yatharth.musitify.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [Index(value = ["name"], unique = true)]
)
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val length: Long,
    val filePath: String,
    val artPath: String,
    val timeHeard: Int,
    val downloadedAt: Long
)