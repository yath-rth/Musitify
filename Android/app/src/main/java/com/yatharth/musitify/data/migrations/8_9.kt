package com.yatharth.musitify.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ListeningSessions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `songId` INTEGER,
                `playlistId` INTEGER,
                `songName` TEXT,
                `artistName` TEXT,
                `playlistName` TEXT,
                `songArtPath` TEXT,
                `songLength` INTEGER NOT NULL,
                `startTimeMs` INTEGER NOT NULL,
                `durationMs` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_ListeningSessions_songId` ON `ListeningSessions` (`songId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_ListeningSessions_playlistId` ON `ListeningSessions` (`playlistId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_ListeningSessions_startTimeMs` ON `ListeningSessions` (`startTimeMs`)"
        )
    }
}