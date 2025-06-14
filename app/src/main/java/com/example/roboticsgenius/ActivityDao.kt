// app/src/main/java/com/example/roboticsgenius/ActivityDao.kt

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

    @Query("SELECT * FROM activities WHERE id = :activityId")
    suspend fun getActivityById(activityId: Int): Activity?

    @Insert
    suspend fun insertTimeLog(log: TimeLogEntry)

    @Query("SELECT * FROM time_log_entries ORDER BY startTime DESC")
    fun getAllTimeLogs(): Flow<List<TimeLogEntry>>

    // NEW FUNCTION: Gets logs for an activity between a start and end time.
    @Query("SELECT * FROM time_log_entries WHERE activityId = :activityId AND startTime >= :startTime AND startTime <= :endTime")
    suspend fun getLogsForActivityInRange(activityId: Int, startTime: Long, endTime: Long): List<TimeLogEntry>

    @Query("""
        SELECT a.name as activityName, t.durationInSeconds, t.startTime
        FROM time_log_entries t
        INNER JOIN activities a ON t.activityId = a.id
        ORDER BY t.startTime DESC
    """)
    fun getAllLogsWithActivityNames(): Flow<List<TimeLogWithActivityName>>

    @Query("SELECT * FROM time_log_entries WHERE activityId = :activityId ORDER BY startTime DESC")
    suspend fun getLogsForActivity(activityId: Int): List<TimeLogEntry>
}