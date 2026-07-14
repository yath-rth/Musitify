package com.yatharth.musitify.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.yatharth.musitify.data.entity.Song

@Dao
interface SongDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSong(song: Song)

}