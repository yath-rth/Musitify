package com.yatharth.musitify.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            Create Table Settings(
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `name` TEXT NOT NULL,
            `value` TEXT NOT NULL,
            `updated_on` INTEGER NOT NULL); 
        """.trimIndent()
        )

        db.execSQL("""
            CREATE INDEX `index_Settings_name`
            ON `Settings` (`name`)
        """)
    }
}
