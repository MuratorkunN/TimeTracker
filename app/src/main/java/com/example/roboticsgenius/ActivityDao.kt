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

    // --- NEW, POWERFUL QUERY ---
    // This query looks at both tables, joins them by the activityId,
    // and returns our new combined data object.
    @Query("""
        SELECT a.name as activityName, t.durationInSeconds, t.startTime
        FROM time_log_entries t
        INNER JOIN activities a ON t.activityId = a.id
        ORDER BY t.startTime DESC
    """)
    fun getAllLogsWithActivityNames(): Flow<List<TimeLogWithActivityName>>
}