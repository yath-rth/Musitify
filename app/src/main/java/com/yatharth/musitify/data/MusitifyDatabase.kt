package com.yatharth.musitify.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabase.Callback
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.yatharth.musitify.data.dao.PlaylistDao
import com.yatharth.musitify.data.dao.SongDao
import com.yatharth.musitify.data.dao.SongToPlaylistDao
import com.yatharth.musitify.data.entity.Playlist
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.entity.SongToPlaylist
import com.yatharth.musitify.util.UUIDConverter
import kotlin.jvm.java

@Database(
    entities = [Song::class, Playlist::class, SongToPlaylist::class],
    version = 4
)
@TypeConverters(UUIDConverter::class)
abstract class MusitifyDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun songToPlaylistDao(): SongToPlaylistDao
}