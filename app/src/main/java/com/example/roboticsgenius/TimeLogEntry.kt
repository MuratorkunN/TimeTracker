// TimeLogEntry.kt
package com.example.roboticsgenius
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "time_log_entries")
data class TimeLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val activityId: Int,
    val startTime: Long,
    val durationInSeconds: Int
)