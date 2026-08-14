package com.yatharth.musitify.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yatharth.musitify.data.entity.Playlist
import com.yatharth.musitify.data.entity.PlaylistInfo
import com.yatharth.musitify.data.entity.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addPlaylist(playlist: Playlist): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addPlaylists(playlist: List<Playlist>)

    @Query(
        """
    SELECT p.*, COUNT(sp.songId) as numOfSongs, SUM(song.length) as playlistLength 
    FROM Playlist p
    LEFT JOIN SongToPlaylist sp ON sp.playListId = p.id
    LEFT JOIN Song song ON song.id = sp.songId
    GROUP BY p.id
    ORDER BY p.createdAt
    """
    )
    fun getAllPlaylists(): Flow<List<PlaylistInfo>>

    @Query(
        """
    SELECT p.*
    FROM Playlist p
    """
    )
    suspend fun getAllPlaylistsSuspend(): List<Playlist>

    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    @Query(
        """
        SELECT s.*
        FROM Song s
        INNER JOIN SongToPlaylist sp
            ON sp.playListId = :playlistId
        WHERE s.id = sp.songId
        ORDER BY sp.position
    """
    )
    fun getSongsForPlaylist(playlistId: Int): Flow<List<Song>>

    @Query("DELETE FROM Playlist WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("UPDATE Playlist SET timeHeard = timeHeard + :ms WHERE id = :id")
    suspend fun addListenTime(id: Int, ms: Long)

    @Query("SELECT * FROM Playlist WHERE name = :name LIMIT 1")
    suspend fun getPlaylistByName(name: String): Playlist?

}