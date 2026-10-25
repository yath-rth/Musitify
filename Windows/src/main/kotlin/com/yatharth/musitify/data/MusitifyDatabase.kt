package com.yatharth.musitify.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
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
import com.yatharth.musitify.platform.AppPaths
import com.yatharth.musitify.util.UUIDConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import java.io.File

/**
 * Same entities/version as Android v9. Fresh desktop DB, so no migrations.
 * Plain JVM target: Room's KSP generates `MusitifyDatabase_Impl`, which
 * `Room.databaseBuilder<T>()` locates reflectively (no @ConstructedBy needed).
 */
@Database(
    entities = [
        Song::class,
        Playlist::class,
        SongToPlaylist::class,
        Settings::class,
        SyncAction::class,
        ListeningSession::class
    ],
    version = 9,
    exportSchema = true
)
@TypeConverters(UUIDConverter::class)
abstract class MusitifyDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun songToPlaylistDao(): SongToPlaylistDao
    abstract fun settingsDao(): SettingsDao
    abstract fun syncActionDao(): SyncActionDao
    abstract fun listeningSessionDao(): ListeningSessionDao

    companion object {
        fun build(file: File = AppPaths.dbFile): MusitifyDatabase =
            Room.databaseBuilder<MusitifyDatabase>(name = file.absolutePath)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
    }
}
