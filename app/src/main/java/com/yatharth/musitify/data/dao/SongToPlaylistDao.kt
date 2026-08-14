package com.yatharth.musitify.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.IGNORE
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.yatharth.musitify.data.entity.SongToPlaylist

@Dao
interface SongToPlaylistDao {

    @Query("SELECT * FROM SongToPlaylist")
    suspend fun getAll(): List<SongToPlaylist>

    @Insert(onConflict = IGNORE)
    suspend fun addSongToPlaylist(songToPlaylist: SongToPlaylist)

    @Insert(onConflict = REPLACE)
    suspend fun addSongsToPlaylist(songToPlaylist: List<SongToPlaylist>)

    @Query(
        """
SELECT COALESCE(MAX(position), -1)
FROM SongToPlaylist
WHERE playListId = :playlistId
"""
    )
    suspend fun getLastPosition(playlistId: Int): Int

    @Query("DELETE FROM SONGTOPLAYLIST WHERE songId = :songId AND playListId = :playlistId")
    suspend fun remove(songId: Int, playlistId: Int)

}