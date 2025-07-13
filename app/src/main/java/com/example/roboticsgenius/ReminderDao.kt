package com.example.roboticsgenius

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    /**
     * Gets all reminders, sorted by the time of day (hour and minute) of their next trigger.
     */
    @Query("SELECT * FROM reminders ORDER BY strftime('%H', nextTriggerTime / 1000, 'unixepoch'), strftime('%M', nextTriggerTime / 1000, 'unixepoch')")
    fun getAllReminders(): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteById(reminderId: Int)

    @Query("SELECT * FROM reminders WHERE id = :reminderId")
    suspend fun getReminderById(reminderId: Int): Reminder?
}