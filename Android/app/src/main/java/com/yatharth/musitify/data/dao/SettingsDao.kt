package com.yatharth.musitify.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.IGNORE
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Update
import com.yatharth.musitify.data.entity.Settings

@Dao
interface SettingsDao {

    @Insert(onConflict = REPLACE)
    suspend fun updateSetting(setting: Settings): Long

    @Query("SELECT * FROM SETTINGS WHERE name = :name")
    suspend fun getSettingByName(name: String): Settings?

    @Query("SELECT * FROM Settings WHERE id = :id")
    suspend fun getSettingsById(id: Int): Settings?
}