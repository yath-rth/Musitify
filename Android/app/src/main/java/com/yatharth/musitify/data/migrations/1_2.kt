package com.yatharth.musitify.data.migrations

import android.graphics.PorterDuff
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "DROP INDEX IF EXISTS `index_Song_name`"
        )

        db.execSQL(
            """
                ALTER TABLE Song ADD COLUMN url TEXT NOT NULL DEFAULT ''
            """
        )

        db.execSQL(
            """
                ALTER TABLE Song ADD COLUMN artist TEXT NOT NULL DEFAULT ''
            """
        )

        db.execSQL(
            "UPDATE `Song` SET `url` = 'legacy_' || `name`"
        )

        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_Song_url` ON `Song` (`url`)"
        )
    }
}
