// app/src/main/java/com/example/roboticsgenius/TimeLogWithActivityName.kt

package com.example.roboticsgenius

data class TimeLogWithActivityName(
    val id: Int,
    val activityName: String,
    val activityColor: String, // NEW: The hex color string of the activity
    val durationInSeconds: Int,
    val startTime: Long
)