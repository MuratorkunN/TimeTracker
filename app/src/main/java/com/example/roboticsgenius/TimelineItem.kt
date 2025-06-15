// app/src/main/java/com/example/roboticsgenius/TimelineItem.kt

package com.example.roboticsgenius

sealed class TimelineItem {
    data class Header(val date: String, val totalDuration: String, val id: String) : TimelineItem()
    // Now holds the full TimeLogWithActivityName object
    data class Log(val logEntry: TimeLogWithActivityName) : TimelineItem()
}