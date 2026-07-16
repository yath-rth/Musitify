package com.yatharth.musitify.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yatharth.musitify.data.entity.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSong(song: Song)

    @Query("SELECT * FROM Song ORDER BY downloadedAt")
    fun getAllSongs(): Flow<List<Song>>

}