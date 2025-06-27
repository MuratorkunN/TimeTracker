// app/src/main/java/com/example/roboticsgenius/AppDatabase.kt

package com.example.roboticsgenius
import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Bumping the version number to 5
@Database(entities = [Activity::class, TimeLogEntry::class, DataSet::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun dataSetDao(): DataSetDao // New DAO accessor

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // Migration from 4 to 5
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create the new data_sets table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `data_sets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `iconName` TEXT NOT NULL,
                        `color` TEXT NOT NULL
                    )
                """)
                // 2. Add a default "General" dataset
                db.execSQL("INSERT INTO data_sets (id, name, iconName, color) VALUES (1, 'General', 'ic_default_dataset', '#808080')")

                // 3. Recreate the activities table with the foreign key
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

                // 4. Copy data from old table to new table
                db.execSQL("""
                    INSERT INTO `activities_new` (id, name, color, targetDurationSeconds, targetPeriod, orderIndex)
                    SELECT id, name, color, targetDurationSeconds, targetPeriod, orderIndex FROM `activities`
                """)

                // 5. Create an index on the new column
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_activities_dataSetId` ON `activities_new` (`dataSetId`)")

                // 6. Drop the old table
                db.execSQL("DROP TABLE `activities`")

                // 7. Rename the new table to the old table's name
                db.execSQL("ALTER TABLE `activities_new` RENAME TO `activities`")
            }
        }


        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "activity_database")
                    .addMigrations(MIGRATION_4_5) // Add the migration path
                    .build().also { INSTANCE = it }
            }
        }
    }
}