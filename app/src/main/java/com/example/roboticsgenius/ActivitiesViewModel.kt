// app/src/main/java/com/example/roboticsgenius/ActivitiesViewModel.kt

package com.example.roboticsgenius

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ActivitiesViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val activityDao = db.activityDao()

    val activitiesUiModel: StateFlow<List<ActivityUiModel>> =
        activityDao.getAllActivities().combine(activityDao.getAllTimeLogs()) { activities, allLogs ->
            activities.map { activity ->
                val relevantLogs = allLogs.filter { it.activityId == activity.id }
                val streak = StreakCalculator.calculateStreak(activity, relevantLogs)
                val statusText = generateStatusText(activity, relevantLogs, streak)

                ActivityUiModel(
                    activity = activity,
                    streakCount = streak,
                    statusText = statusText
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteActivity(activity: Activity) {
        viewModelScope.launch {
            activityDao.deleteActivityAndLogs(activity)
        }
    }

    fun updateActivityOrder(activities: List<ActivityUiModel>) {
        viewModelScope.launch {
            val updatedActivities = activities.mapIndexed { index, uiModel ->
                uiModel.activity.copy(orderIndex = index)
            }
            activityDao.updateActivities(updatedActivities)
        }
    }

    fun updateActivity(activity: Activity) {
        viewModelScope.launch {
            activityDao.updateActivity(activity)
        }
    }

    // NEW: Function to save a manually entered log
    fun addManualLog(activityId: Int, startTime: Long, durationInSeconds: Int) {
        viewModelScope.launch {
            val logEntry = TimeLogEntry(
                activityId = activityId,
                startTime = startTime,
                durationInSeconds = durationInSeconds
            )
            activityDao.insertTimeLog(logEntry)
        }
    }

    private fun generateStatusText(activity: Activity, logs: List<TimeLogEntry>, streak: Int): String {
        if (activity.targetDurationSeconds <= 0) {
            return "No target set"
        }
        val (start, end) = getPeriodTimestamps(activity.targetPeriod)
        val durationInPeriod = logs.filter { it.startTime in start..end }.sumOf { it.durationInSeconds }
        if (durationInPeriod >= activity.targetDurationSeconds) {
            return if (streak > 0) "You're on a ${streak}-day streak!" else "Target met!"
        }
        val remainingSeconds = activity.targetDurationSeconds - durationInPeriod
        val hours = TimeUnit.SECONDS.toHours(remainingSeconds.toLong())
        val minutes = TimeUnit.SECONDS.toMinutes(remainingSeconds.toLong()) % 60
        val timeStr = when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${remainingSeconds}s"
        }
        return "$timeStr left until ${activity.targetPeriod.lowercase()} target"
    }

    private fun getPeriodTimestamps(period: String): Pair<Long, Long> {
        val now = Calendar.getInstance()
        val start = now.clone() as Calendar
        when (period) {
            "Daily" -> {
                start.set(Calendar.HOUR_OF_DAY, 0)
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
            }
            "Weekly" -> {
                start.firstDayOfWeek = Calendar.MONDAY
                start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                start.set(Calendar.HOUR_OF_DAY, 0)
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
            }
            "Monthly" -> {
                start.set(Calendar.DAY_OF_MONTH, 1)
                start.set(Calendar.HOUR_OF_DAY, 0)
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
            }
        }
        return Pair(start.timeInMillis, now.timeInMillis)
    }
}