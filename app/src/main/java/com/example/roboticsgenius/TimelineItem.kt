package com.example.roboticsgenius

sealed class TimelineItem {
    data class Header(val date: String, val totalDuration: String, val id: String) : TimelineItem()
    data class Log(val logEntry: TimeLogWithActivityName, val id: Long) : TimelineItem()
}