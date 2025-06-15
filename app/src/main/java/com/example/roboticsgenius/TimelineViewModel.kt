// app/src/main/java/com/example/roboticsgenius/TimelineViewModel.kt

package com.example.roboticsgenius

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class TimelineViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val activityDao = db.activityDao()

    val timelineItems: StateFlow<List<TimelineItem>> =
        activityDao.getAllLogsWithActivityNames()
            .map { logs -> processLogsIntoTimelineItems(logs) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun deleteLog(logId: Int) {
        viewModelScope.launch {
            activityDao.deleteTimeLogById(logId)
        }
    }

    private fun processLogsIntoTimelineItems(logs: List<TimeLogWithActivityName>): List<TimelineItem> {
        if (logs.isEmpty()) {
            return emptyList()
        }

        val items = mutableListOf<TimelineItem>()
        val groupedLogs = logs.groupBy { getCalendarDate(it.startTime) }

        for ((date, dailyLogs) in groupedLogs) {
            val totalDurationSeconds = dailyLogs.sumOf { it.durationInSeconds }
            items.add(
                TimelineItem.Header(
                    id = date.time.toString(),
                    date = formatDateHeader(date),
                    totalDuration = formatDurationHeader(totalDurationSeconds)
                )
            )
            dailyLogs.forEach { log ->
                items.add(TimelineItem.Log(logEntry = log))
            }
        }
        return items
    }

    private fun getCalendarDate(timestamp: Long): Calendar {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun formatDateHeader(cal: Calendar): String {
        val today = getCalendarDate(System.currentTimeMillis())
        val yesterday = (today.clone() as Calendar).apply { add(Calendar.DATE, -1) }

        return when (cal) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(cal.time)
        }
    }

    private fun formatDurationHeader(totalSeconds: Int): String {
        if (totalSeconds < 0) return ""
        val hours = TimeUnit.SECONDS.toHours(totalSeconds.toLong())
        val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds.toLong()) % 60
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }
}