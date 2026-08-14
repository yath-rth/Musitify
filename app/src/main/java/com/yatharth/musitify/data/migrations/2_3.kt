package com.yatharth.musitify.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yatharth.musitify.data.entity.Playlist
import com.yatharth.musitify.data.entity.SongToPlaylist

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `Song` ADD COLUMN `uuid` TEXT"
        )

        db.execSQL(
            """
                ALTER TABLE Playlist ADD COLUMN `uuid` TEXT
            """
        )

        db.execSQL(
            "ALTER TABLE `SongToPlaylist` ADD COLUMN `song_uuid` TEXT"
        )
        
        db.execSQL(
            "ALTER TABLE `SongToPlaylist` ADD COLUMN `playlist_uuid` TEXT"
        )
    }
}
