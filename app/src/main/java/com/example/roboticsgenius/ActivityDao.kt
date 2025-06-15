// app/src/main/java/com/example/roboticsgenius/ActivityDao.kt

package com.example.roboticsgenius

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Insert suspend fun insert(activity: Activity)
    @Update suspend fun updateActivities(activities: List<Activity>)
    @Update suspend fun updateActivity(activity: Activity)
    @Delete suspend fun delete(activity: Activity)
    @Query("DELETE FROM time_log_entries WHERE activityId = :activityId")
    suspend fun deleteLogsForActivity(activityId: Int)
    @Transaction
    suspend fun deleteActivityAndLogs(activity: Activity) {
        deleteLogsForActivity(activity.id)
        delete(activity)
    }
    @Query("SELECT * FROM activities ORDER BY orderIndex ASC")
    fun getAllActivities(): Flow<List<Activity>>
    @Query("SELECT MAX(orderIndex) FROM activities")
    suspend fun getMaxOrderIndex(): Int?
    @Query("SELECT * FROM activities WHERE id = :activityId")
    suspend fun getActivityById(activityId: Int): Activity?
    @Insert suspend fun insertTimeLog(log: TimeLogEntry)
    @Query("DELETE FROM time_log_entries WHERE id = :logId")
    suspend fun deleteTimeLogById(logId: Int)
    @Query("SELECT * FROM time_log_entries ORDER BY startTime DESC")
    fun getAllTimeLogs(): Flow<List<TimeLogEntry>>
    @Query("SELECT * FROM time_log_entries WHERE activityId = :activityId AND startTime >= :startTime AND startTime <= :endTime")
    suspend fun getLogsForActivityInRange(activityId: Int, startTime: Long, endTime: Long): List<TimeLogEntry>

    // THE FIX: Also select the color from the activities table
    @Query("""
        SELECT 
            t.id, 
            a.name as activityName, 
            a.color as activityColor,
            t.durationInSeconds, 
            t.startTime
        FROM time_log_entries t
        INNER JOIN activities a ON t.activityId = a.id
        ORDER BY t.startTime DESC
    """)
    fun getAllLogsWithActivityNames(): Flow<List<TimeLogWithActivityName>>
    @Query("SELECT * FROM time_log_entries WHERE activityId = :activityId ORDER BY startTime DESC")
    suspend fun getLogsForActivity(activityId: Int): List<TimeLogEntry>
}