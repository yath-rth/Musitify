package com.yatharth.musitify.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yatharth.musitify.data.entity.Playlist
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addPlaylist(playlist: Playlist): Long

    @Query("SELECT * FROM Playlist ORDER BY createdAt")
    fun getAllPlaylists(): Flow<List<Playlist>>

}