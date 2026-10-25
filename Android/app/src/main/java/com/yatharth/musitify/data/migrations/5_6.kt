package com.yatharth.musitify.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
    DROP INDEX IF EXISTS `index_Settings_name`
""")

        db.execSQL("""
    CREATE UNIQUE INDEX `index_Settings_name`
    ON `Settings` (`name`)
""")
    }
}