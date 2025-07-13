package com.example.roboticsgenius
import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Bumping the version number to 9 for the new notes table
@Database(entities = [Activity::class, TimeLogEntry::class, DataSet::class, DataEntry::class, Reminder::class, Note::class], version = 9, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun dataSetDao(): DataSetDao
    abstract fun dataEntryDao(): DataEntryDao
    abstract fun reminderDao(): ReminderDao
    abstract fun noteDao(): NoteDao // New DAO for Notes

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `data_sets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `iconName` TEXT NOT NULL,
                        `color` TEXT NOT NULL
                    )
                """)
                db.execSQL("INSERT INTO data_sets (id, name, iconName, color) VALUES (1, 'General', 'ic_academics', '#808080')")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `activities_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `color` TEXT NOT NULL,
                        `targetDurationSeconds` INTEGER NOT NULL,
                        `targetPeriod` TEXT NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        `dataSetId` INTEGER NOT NULL DEFAULT 1,
                        FOREIGN KEY(`dataSetId`) REFERENCES `data_sets`(`id`) ON UPDATE NO ACTION ON DELETE SET DEFAULT
                    )
                """)
                db.execSQL("""
                    INSERT INTO `activities_new` (id, name, color, targetDurationSeconds, targetPeriod, orderIndex)
                    SELECT id, name, color, targetDurationSeconds, targetPeriod, orderIndex FROM `activities`
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_activities_dataSetId` ON `activities_new` (`dataSetId`)")
                db.execSQL("DROP TABLE `activities`")
                db.execSQL("ALTER TABLE `activities_new` RENAME TO `activities`")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `activities` ADD COLUMN `isTimeTrackerActivity` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `activities` ADD COLUMN `dataType` TEXT NOT NULL DEFAULT 'Text'")
                db.execSQL("ALTER TABLE `activities` ADD COLUMN `dataOptions` TEXT")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `activities` ADD COLUMN `decimalPlaces` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `activities` ADD COLUMN `isSingleSelection` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `data_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `activityId` INTEGER NOT NULL, 
                        `date` TEXT NOT NULL, 
                        `value` TEXT NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `reminders` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `message` TEXT NOT NULL,
                        `nextTriggerTime` INTEGER NOT NULL,
                        `repeatDays` INTEGER NOT NULL,
                        `color` TEXT NOT NULL,
                        `isEnabled` INTEGER NOT NULL DEFAULT 1
                    )
                """)
            }
        }

        // New migration to add the notes table
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `color` TEXT NOT NULL,
                        `lastModified` INTEGER NOT NULL
                    )
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "activity_database")
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9) // Add new migration
                    .build().also { INSTANCE = it }
            }
        }
    }
}