package com.example.roboticsgenius

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_log_entries")
data class TimeLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val activityId: Int,
    val startTime: Long, // We'll store the start time as a timestamp
    val durationInSeconds: Int
)