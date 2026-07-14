package com.yatharth.musitify.data

import android.content.Context
import androidx.room.Room
import com.yatharth.musitify.data.dao.PlaylistDao
import com.yatharth.musitify.data.dao.SongDao
import com.yatharth.musitify.data.dao.SongToPlaylistDao
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
        ).build()
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
}