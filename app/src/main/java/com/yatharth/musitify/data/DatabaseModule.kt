package com.yatharth.musitify.data

import android.content.Context
import androidx.room.Room
import com.yatharth.musitify.data.dao.PlaylistDao
import com.yatharth.musitify.data.dao.SettingsDao
import com.yatharth.musitify.data.dao.SongDao
import com.yatharth.musitify.data.dao.SongToPlaylistDao
import com.yatharth.musitify.data.dao.SyncActionDao
import com.yatharth.musitify.data.migrations.MIGRATION_1_2
import com.yatharth.musitify.data.migrations.MIGRATION_2_3
import com.yatharth.musitify.data.migrations.MIGRATION_3_4
import com.yatharth.musitify.data.migrations.MIGRATION_4_5
import com.yatharth.musitify.data.migrations.MIGRATION_5_6
import com.yatharth.musitify.data.migrations.MIGRATION_6_7
import com.yatharth.musitify.data.migrations.MIGRATION_7_8
import com.yatharth.musitify.util.UUIDConverter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): MusitifyDatabase {

        return Room.databaseBuilder(
            context,
            MusitifyDatabase::class.java,
            "musitify_database"
        )
            .addMigrations(
                MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8
            )
            .build()
    }

    @Provides
    fun provideSongDao(
        db: MusitifyDatabase
    ): SongDao = db.songDao()

    @Provides
    fun providePlaylistDao(
        db: MusitifyDatabase
    ): PlaylistDao = db.playlistDao()

    @Provides
    fun provideSongToPlaylistDao(
        db: MusitifyDatabase
    ): SongToPlaylistDao = db.songToPlaylistDao()

    @Provides
    fun provideSettingsDao(
        db: MusitifyDatabase
    ): SettingsDao = db.settingsDao()

    @Provides
    fun provideSyncActionDao(
        db: MusitifyDatabase
    ): SyncActionDao = db.syncActionDao()
}