package com.yatharth.musitify.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yatharth.musitify.data.entity.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSong(song: Song): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongs(song: List<Song>)

    @Query("SELECT * FROM Song ORDER BY downloadedAt")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM Song ORDER BY downloadedAt")
    suspend fun getAllSongsSuspend(): List<Song>

    @Query("SELECT * FROM Song WHERE length = 0 ORDER BY downloadedAt")
    suspend fun getAllSongsWithoutLength(): List<Song>

    @Query("SELECT * FROM Song WHERE id = :id")
    suspend fun getSongById(id: Int): Song?

    @Query("SELECT * FROM Song WHERE uuid = :id")
    suspend fun getSongByUuid(id: String): Song?

    @Query("SELECT * FROM Song WHERE name = :name LIMIT 1")
    suspend fun getSongByName(name: String): Song?

    @Query("SELECT * FROM Song WHERE url = :url")
    suspend fun getSongByUrl(url: String): Song?

    @Update
    suspend fun updateSong(song: Song)

    @Query(
        """
        SELECT *
        FROM Song
        WHERE id in (:ids)
        ORDER BY downloadedAt
    """
    )
    suspend fun getSongsByIds(ids: List<Int>): List<Song>

    @Query(
        """
        SELECT *
        FROM Song
        WHERE url in (:urls)
        ORDER BY downloadedAt
    """
    )
    suspend fun getSongsByUrls(urls: List<String>): List<Song>

    @Query("SELECT * FROM Song WHERE name LIKE '%' || :query || '%' ORDER BY name")
    fun searchSongs(query: String): Flow<List<Song>>

    @Query("DELETE FROM Song WHERE id = :id")
    suspend fun deleteSongById(id: Int)

    @Query("DELETE FROM Song WHERE uuid = :id")
    suspend fun deleteSongByUuid(id: String)

    @Query("UPDATE Song SET timeHeard = timeHeard + :ms WHERE id = :songId")
    suspend fun addListenTime(songId: Int, ms: Long)

    @Query("UPDATE Song SET length = :length WHERE id = :songId")
    suspend fun updateLength(songId: Int, length: Long)

    @Query("SELECT COUNT(*) FROM Song")
    fun getTotalNumOfSongs(): Flow<Long>
}