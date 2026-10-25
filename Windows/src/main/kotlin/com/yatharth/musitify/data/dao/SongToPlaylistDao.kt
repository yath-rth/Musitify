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

    @Insert(onConflict = REPLACE)
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

    @Delete
    suspend fun removeSong(songPlaylist: SongToPlaylist)

    @Query("DELETE FROM SONGTOPLAYLIST WHERE songId = :songId AND playListId = :playlistId")
    suspend fun remove(songId: Int, playlistId: Int)

    @Query("SELECT * FROM SONGTOPLAYLIST where song_uuid = :songUuid AND playlist_uuid = :playlistUuid")
    suspend fun findBySongUuidAndPlaylistUuid(
        songUuid: String,
        playlistUuid: String
    ): SongToPlaylist?

    @Query("UPDATE SongToPlaylist SET position = :newPosition WHERE songId = :songId AND playListId = :playlistId")
    suspend fun updatePosition(songId: Int, playlistId: Int, newPosition: Int)

    @Query("SELECT * FROM SongToPlaylist WHERE playListId = :playlistId ORDER BY position")
    suspend fun getSongsInPlaylist(playlistId: Int): List<SongToPlaylist>

    // ---- Desktop additions (sync fixes) ----
    // Note: with the desktop-only UNIQUE(songId, playListId) index, the REPLACE
    // inserts above replace an existing relation instead of duplicating it.

    /** Inserts relations, skipping any (songId, playListId) pair that already exists. Returns -1 for skipped rows. */
    @Insert(onConflict = IGNORE)
    suspend fun addSongsToPlaylistIfAbsent(songToPlaylist: List<SongToPlaylist>): List<Long>

    /** Inserts one relation unless it already exists. Returns -1 when skipped. */
    @Insert(onConflict = IGNORE)
    suspend fun addSongToPlaylistIfAbsent(songToPlaylist: SongToPlaylist): Long

    @Query("SELECT * FROM SongToPlaylist WHERE songId = :songId AND playListId = :playlistId")
    suspend fun find(songId: Int, playlistId: Int): SongToPlaylist?

    @Query("SELECT songId FROM SongToPlaylist WHERE playListId = :playlistId ORDER BY position")
    suspend fun getSongIdsInPlaylist(playlistId: Int): List<Int>
}