package com.example.roboticsgenius

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Insert
    suspend fun insert(activity: Activity)

    @Query("SELECT * FROM activities ORDER BY name ASC")
    fun getAllActivities(): Flow<List<Activity>>

    @Insert
    suspend fun insertTimeLog(log: TimeLogEntry)

    // This is the function for the HISTORY/TIMELINE screen. It was missing.
    @Query("""
        SELECT a.name as activityName, t.durationInSeconds, t.startTime
        FROM time_log_entries t
        INNER JOIN activities a ON t.activityId = a.id
        ORDER BY t.startTime DESC
    """)
    fun getAllLogsWithActivityNames(): Flow<List<TimeLogWithActivityName>>

    // This is the function for the STREAK calculation. It was missing.
    @Query("SELECT * FROM time_log_entries WHERE activityId = :activityId ORDER BY startTime DESC")
    suspend fun getLogsForActivity(activityId: Int): List<TimeLogEntry>
}