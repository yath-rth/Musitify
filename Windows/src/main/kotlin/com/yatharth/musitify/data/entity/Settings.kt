package com.yatharth.musitify.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Settings",
    indices = [Index(value = ["name"], unique = true)]
)
data class Settings(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val value: String,
    val updated_on: Long
)
