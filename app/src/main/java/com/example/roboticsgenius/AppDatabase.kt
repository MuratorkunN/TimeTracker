// app/src/main/java/com/example/roboticsgenius/AppDatabase.kt

package com.example.roboticsgenius
import android.content.Context
import androidx.room.*

// THE FIX: Bumping the version number to 4
@Database(entities = [Activity::class, TimeLogEntry::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "activity_database")
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}