package com.yatharth.musitify.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.IGNORE
import com.yatharth.musitify.data.entity.SongToPlaylist

@Dao
interface SongToPlaylistDao {

    @Insert(onConflict = IGNORE)
    suspend fun addSongToPlaylist(songToPlaylist: SongToPlaylist)

    @Insert(onConflict = IGNORE)
    suspend fun addSongsToPlaylist(songToPlaylist: List<SongToPlaylist>)

}