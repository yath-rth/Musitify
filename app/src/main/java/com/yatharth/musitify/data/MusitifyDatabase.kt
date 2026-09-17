package com.yatharth.musitify.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabase.Callback
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.yatharth.musitify.data.dao.ListeningSessionDao
import com.yatharth.musitify.data.dao.PlaylistDao
import com.yatharth.musitify.data.dao.SettingsDao
import com.yatharth.musitify.data.dao.SongDao
import com.yatharth.musitify.data.dao.SongToPlaylistDao
import com.yatharth.musitify.data.dao.SyncActionDao
import com.yatharth.musitify.data.entity.ListeningSession
import com.yatharth.musitify.data.entity.Playlist
import com.yatharth.musitify.data.entity.Settings
import com.yatharth.musitify.data.entity.Song
import com.yatharth.musitify.data.entity.SongToPlaylist
import com.yatharth.musitify.data.entity.SyncAction
import com.yatharth.musitify.util.UUIDConverter
import kotlin.jvm.java

@Database(
    entities = [
        Song::class,
        Playlist::class,
        SongToPlaylist::class,
        Settings::class,
        SyncAction::class,
        ListeningSession::class
    ],
    version = 9
)
@TypeConverters(UUIDConverter::class)
abstract class MusitifyDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun songToPlaylistDao(): SongToPlaylistDao
    abstract fun settingsDao(): SettingsDao
    abstract fun syncActionDao(): SyncActionDao
    abstract fun listeningSessionDao(): ListeningSessionDao
}