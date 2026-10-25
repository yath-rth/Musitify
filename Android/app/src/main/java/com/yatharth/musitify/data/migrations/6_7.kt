package com.yatharth.musitify.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
    CREATE UNIQUE INDEX `index_Song_uuid`
    ON `Song` (`uuid`)
""")

        db.execSQL("""
    CREATE UNIQUE INDEX `index_Playlist_name`
    ON `Playlist` (`name`)
""")
    }
}