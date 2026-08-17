package com.yatharth.musitify.data.entity

import android.R.attr.data
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.util.TableInfo

@Entity(
    tableName = "Settings",
    indices = [Index(value = ["name"])]
)
data class Settings(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val value: String,
    val updated_on: Long
)
