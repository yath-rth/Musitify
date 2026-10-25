package com.yatharth.musitify.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `Actions` (
                `localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `actionUuid` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `songUuid` TEXT,
                `playlistUuid` TEXT,
                `payload` TEXT,
                `createdAt` INTEGER NOT NULL,
                `isSynced` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_Actions_actionUuid` ON `Actions` (`actionUuid`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_Actions_isSynced` ON `Actions` (`isSynced`)"
        )
    }
}
