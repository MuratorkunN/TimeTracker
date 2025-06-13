package com.example.roboticsgenius

// A simple data class to hold the combined information from our database query
data class TimeLogWithActivityName(
    val activityName: String,
    val durationInSeconds: Int,
    val startTime: Long
)