package com.example.roboticsgenius

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// --- UPDATED DATABASE ANNOTATION ---
@Database(entities = [Activity::class, TimeLogEntry::class], version = 2) // Version is now 2!
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "activity_database"
                )
                    // --- This tells Room how to handle the upgrade from v1 to v2 ---
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // --- A migration path to create the new table without losing old data ---
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `time_log_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `activityId` INTEGER NOT NULL, `startTime` INTEGER NOT NULL, `durationInSeconds` INTEGER NOT NULL)"
                )
            }
        }
    }
}