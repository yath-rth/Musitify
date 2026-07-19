package com.yatharth.musitify.data.dao

import androidx.room.Dao
import androidx.room.Delete
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

    @Query("SELECT * FROM Song WHERE id = :id")
    suspend fun getSongById(id: Int): Song?

    @Query(
        """
        SELECT *
        FROM Song
        WHERE id in (:ids)
        ORDER BY downloadedAt
    """
    )
    suspend fun getSongsByIds(ids: List<Int>): List<Song>

    @Query("DELETE FROM Song WHERE id = :id")
    suspend fun deleteSong(id: Int)

    @Query("UPDATE Song SET timeHeard = timeHeard + :ms WHERE id = :songId")
    suspend fun addListenTime(songId: Int, ms: Long)

}