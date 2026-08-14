package com.yatharth.musitify.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    indices = [Index(value = ["url"], unique = true)]
)
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val artist: String,
    val length: Long,
    val filePath: String,
    val artPath: String,
    val timeHeard: Int,
    val downloadedAt: Long,
    val url: String,
    var uuid: UUID? = null
)