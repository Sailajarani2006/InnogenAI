package com.innogen.aipro.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * FIX-09 (MED-005): Explicit Room migrations replace fallbackToDestructiveMigration().
 *
 * fallbackToDestructiveMigration() silently deletes all user data on schema change.
 * Explicit migrations guarantee safe schema upgrades without data loss.
 *
 * To add a new migration:
 *  1. Bump the version in @Database(version = N)
 *  2. Add MIGRATION_N-1_N below
 *  3. Register it in AppModule.provideDatabase()
 */
object DatabaseMigrations {

    /**
     * v1 → v2: Initial migration introduced when SQLCipher encryption was added.
     * No schema changes — the database structure is identical, but from this
     * version forward the file will be opened with SQLCipher instead of plain SQLite.
     *
     * Room itself handles re-encrypting an existing plain database when
     * SupportFactory is first applied; this migration is a no-op schema step
     * required to satisfy Room's version tracking.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // No schema changes in this version.
            // SQLCipher encryption is applied transparently by the SupportFactory.
        }
    }

    // Future migrations — add here as app schema evolves:
    // val MIGRATION_2_3 = object : Migration(2, 3) {
    //     override fun migrate(database: SupportSQLiteDatabase) {
    //         database.execSQL("ALTER TABLE projects ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
    //     }
    // }
}
