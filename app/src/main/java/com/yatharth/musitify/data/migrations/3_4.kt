package com.yatharth.musitify.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // Disable FK checks while rebuilding the tables
        db.execSQL("PRAGMA foreign_keys = OFF")

        // -------------------------
        // Song
        // -------------------------
        db.execSQL("""
            CREATE TABLE `Song_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `artist` TEXT NOT NULL,
                `length` INTEGER NOT NULL,
                `filePath` TEXT NOT NULL,
                `artPath` TEXT NOT NULL,
                `timeHeard` INTEGER NOT NULL,
                `downloadedAt` INTEGER NOT NULL,
                `url` TEXT NOT NULL,
                `uuid` TEXT
            )
        """)

        db.execSQL("""
            INSERT INTO `Song_new`
            (`id`, `name`, `artist`, `length`, `filePath`, `artPath`,
             `timeHeard`, `downloadedAt`, `url`, `uuid`)
            SELECT
            `id`, `name`, `artist`, `length`, `filePath`, `artPath`,
            `timeHeard`, `downloadedAt`, `url`,
            CASE WHEN `uuid` = '' THEN NULL ELSE `uuid` END
            FROM `Song`
        """)

        // -------------------------
        // Playlist
        // -------------------------
        db.execSQL("""
            CREATE TABLE `Playlist_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `timeHeard` INTEGER NOT NULL,
                `artPath` TEXT,
                `createdAt` INTEGER NOT NULL,
                `uuid` TEXT
            )
        """)

        db.execSQL("""
            INSERT INTO `Playlist_new`
            (`id`, `name`, `timeHeard`, `artPath`, `createdAt`, `uuid`)
            SELECT
            `id`, `name`, `timeHeard`, `artPath`, `createdAt`,
            CASE WHEN `uuid` = '' THEN NULL ELSE `uuid` END
            FROM `Playlist`
        """)

        // -------------------------
        // SongToPlaylist
        // -------------------------
        db.execSQL("""
            CREATE TABLE `SongToPlaylist_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `songId` INTEGER NOT NULL,
                `playListId` INTEGER NOT NULL,
                `position` INTEGER NOT NULL,
                `addedOn` INTEGER NOT NULL,
                `song_uuid` TEXT,
                `playlist_uuid` TEXT,
                FOREIGN KEY(`songId`) REFERENCES `Song`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`playListId`) REFERENCES `Playlist`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """)

        db.execSQL("""
            INSERT INTO `SongToPlaylist_new`
            (`id`, `songId`, `playListId`, `position`, `addedOn`,
             `song_uuid`, `playlist_uuid`)
            SELECT
            `id`, `songId`, `playListId`, `position`, `addedOn`,
            CASE WHEN `song_uuid` = '' THEN NULL ELSE `song_uuid` END,
            CASE WHEN `playlist_uuid` = '' THEN NULL ELSE `playlist_uuid` END
            FROM `SongToPlaylist`
        """)

        // Drop old tables. Child table first because of FKs.
        db.execSQL("DROP TABLE `SongToPlaylist`")
        db.execSQL("DROP TABLE `Song`")
        db.execSQL("DROP TABLE `Playlist`")

        // Rename rebuilt tables
        db.execSQL("ALTER TABLE `Song_new` RENAME TO `Song`")
        db.execSQL("ALTER TABLE `Playlist_new` RENAME TO `Playlist`")
        db.execSQL("ALTER TABLE `SongToPlaylist_new` RENAME TO `SongToPlaylist`")

        // Recreate Song's unique URL index
        db.execSQL("""
            CREATE UNIQUE INDEX `index_Song_url`
            ON `Song` (`url`)
        """)

        db.execSQL("PRAGMA foreign_keys = ON")
    }
}
