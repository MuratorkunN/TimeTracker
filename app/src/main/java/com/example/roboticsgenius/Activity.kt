package com.example.roboticsgenius

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class Activity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val color: String, // e.g., "#FF0000" for red
    val targetDurationSeconds: Int, // 0 if no target
    val targetPeriod: String // "Daily", "Weekly", "Monthly"
)