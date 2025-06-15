// app/src/main/java/com/example/roboticsgenius/InsightsViewModel.kt

package com.example.roboticsgenius

import android.app.Application
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

class InsightsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val activityDao = db.activityDao()

    val allActivities: StateFlow<List<Activity>> = activityDao.getAllActivities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _filterState = MutableStateFlow(InsightsFilterState())
    val filterState = _filterState.asStateFlow()

    private val _viewDate = MutableStateFlow(Calendar.getInstance())

    val uiState: StateFlow<InsightsState> = combine(
        _filterState,
        _viewDate,
        allActivities,
        activityDao.getAllTimeLogs()
    ) { filters, viewDate, activities, logs ->
        if (filters.activityIds.isEmpty() && activities.isNotEmpty()) {
            val allIds = activities.map { it.id }.toSet()
            viewModelScope.launch {
                _filterState.value = filters.copy(activityIds = allIds)
            }
            return@combine InsightsState()
        }
        createInsightsUiState(filters, viewDate, activities, logs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InsightsState()
    )

    fun applyFilters(timeRange: TimeRange, activityIds: Set<Int>) {
        _viewDate.value = Calendar.getInstance()
        _filterState.value = InsightsFilterState(timeRange, activityIds)
    }

    // THE ARROW FIX: The arrows now know what period they are in.
    fun navigateForward() {
        _viewDate.update {
            (it.clone() as Calendar).apply {
                val (field, amount) = getNavigationIncrement()
                add(field, amount)
            }
        }
    }

    fun navigateBackward() {
        _viewDate.update {
            (it.clone() as Calendar).apply {
                val (field, amount) = getNavigationIncrement()
                add(field, -amount)
            }
        }
    }

    private fun getNavigationIncrement(): Pair<Int, Int> {
        return when (_filterState.value.timeRange) {
            TimeRange.WEEK -> Pair(Calendar.WEEK_OF_YEAR, 1)
            TimeRange.MONTH -> Pair(Calendar.MONTH, 1)
            TimeRange.SIX_MONTHS -> Pair(Calendar.MONTH, 6)
            TimeRange.YEAR -> Pair(Calendar.YEAR, 1)
        }
    }

    private fun createInsightsUiState(filters: InsightsFilterState, viewDate: Calendar, activities: List<Activity>, logs: List<TimeLogEntry>): InsightsState {
        // THE DATE FIX: This function now ONLY uses the passed-in `viewDate` and never `now`.
        val (startDate, endDate, labelFormat, incrementField, incrementAmount) = getPeriodConfig(filters.timeRange, viewDate)

        val dateLabel = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(startDate.time) + " - " +
                SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(endDate.time)

        val filteredActivities = activities.filter { filters.activityIds.contains(it.id) }.sortedBy { it.orderIndex }
        val filteredLogs = logs.filter { it.startTime in startDate.timeInMillis..endDate.timeInMillis && filters.activityIds.contains(it.activityId) }

        val labels = mutableListOf<String>()
        val dataSets = mutableListOf<ILineDataSet>()
        var overallMaxMinutes = 0f

        // --- THE "LABELS. LABELS. LABELS." FIX ---
        for (activity in filteredActivities) {
            val entries = mutableListOf<Entry>()
            var currentLabelDate = startDate.clone() as Calendar
            var xIndex = 0

            // Loop through the entire period to generate the shape of the data line
            while (!currentLabelDate.after(endDate) && xIndex < 500) {
                // Generate the label for the first activity only. This GUARANTEES the labels are always created.
                if (activities.indexOf(activity) == 0) {
                    labels.add(labelFormat.format(currentLabelDate.time))
                }

                // Check if this date is in the future
                if (currentLabelDate.after(Calendar.getInstance())) {
                    // It IS in the future. DO NOT ADD AN ENTRY. The line will stop.
                } else {
                    // It is in the past or today. ALWAYS ADD AN ENTRY, even if it's zero.
                    val periodEnd = (currentLabelDate.clone() as Calendar).apply { add(incrementField, incrementAmount); add(Calendar.MILLISECOND, -1) }
                    val totalMinutes = filteredLogs
                        .filter { it.activityId == activity.id && it.startTime in currentLabelDate.timeInMillis..periodEnd.timeInMillis }
                        .sumOf { it.durationInSeconds } / 60f

                    if (totalMinutes > overallMaxMinutes) overallMaxMinutes = totalMinutes
                    entries.add(Entry(xIndex.toFloat(), totalMinutes))
                }

                xIndex++
                currentLabelDate.add(incrementField, incrementAmount)
            }

            // Add the dataset for this activity to the list
            dataSets.add(LineDataSet(entries, activity.name).apply {
                color = Color.parseColor(activity.color)
                setCircleColor(Color.parseColor(activity.color))
                circleRadius = 4f
                setDrawCircleHole(false)
                lineWidth = 2.5f
                setDrawValues(false)
                mode = LineDataSet.Mode.LINEAR
            })
        }

        val yAxisMax = when {
            overallMaxMinutes <= 10f -> 15f
            overallMaxMinutes <= 50f -> 60f
            else -> ceil(overallMaxMinutes / 30f) * 30f
        }

        val summaryList = filteredActivities.mapNotNull { activity ->
            val totalSeconds = filteredLogs.filter { it.activityId == activity.id }.sumOf { it.durationInSeconds }
            if (totalSeconds > 0) InsightSummaryItem(activity.name, formatSummaryDuration(totalSeconds), activity.color) else null
        }

        return InsightsState(LineData(dataSets), dateLabel, labels, summaryList, yAxisMax, true)
    }

    private fun getPeriodConfig(timeRange: TimeRange, dateForPeriod: Calendar): PeriodConfig {
        val startDate = dateForPeriod.clone() as Calendar
        val endDate = dateForPeriod.clone() as Calendar
        val labelFormat: SimpleDateFormat
        val incrementField: Int
        val incrementAmount: Int
        resetToStartOfDay(startDate)
        resetToEndOfDay(endDate)

        when (timeRange) {
            TimeRange.WEEK -> {
                startDate.firstDayOfWeek = Calendar.MONDAY
                startDate.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                endDate.timeInMillis = startDate.timeInMillis
                endDate.add(Calendar.DAY_OF_YEAR, 6)
                resetToEndOfDay(endDate)
                labelFormat = SimpleDateFormat("EEE", Locale.getDefault())
                incrementField = Calendar.DAY_OF_YEAR
                incrementAmount = 1
            }
            else -> {
                startDate.set(Calendar.DAY_OF_MONTH, 1)
                val (calField, amount) = when (timeRange) {
                    TimeRange.MONTH -> Pair(Calendar.MONTH, 1)
                    TimeRange.SIX_MONTHS -> Pair(Calendar.MONTH, 6).also { startDate.add(Calendar.MONTH, -5) }
                    TimeRange.YEAR -> Pair(Calendar.YEAR, 1).also { startDate.set(Calendar.DAY_OF_YEAR, 1) }
                    else -> Pair(Calendar.WEEK_OF_YEAR, 1)
                }
                endDate.timeInMillis = startDate.timeInMillis
                endDate.add(calField, amount)
                endDate.add(Calendar.MILLISECOND, -1)
                labelFormat = if (timeRange == TimeRange.MONTH) SimpleDateFormat("d", Locale.getDefault()) else SimpleDateFormat("MMM", Locale.getDefault())
                incrementField = if (timeRange == TimeRange.MONTH) Calendar.DAY_OF_YEAR else Calendar.MONTH
                incrementAmount = 1
            }
        }
        return PeriodConfig(startDate, endDate, labelFormat, incrementField, incrementAmount)
    }

    private fun formatSummaryDuration(totalSeconds: Int): String {
        if (totalSeconds <= 0) return ""
        val hours = TimeUnit.SECONDS.toHours(totalSeconds.toLong()); val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds.toLong()) % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
    private fun resetToStartOfDay(cal: Calendar) { cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0) }
    private fun resetToEndOfDay(cal: Calendar) { cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999) }
    data class PeriodConfig(val start: Calendar, val end: Calendar, val format: SimpleDateFormat, val incrementField: Int, val incrementAmount: Int)
}