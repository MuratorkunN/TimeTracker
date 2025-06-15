package com.example.roboticsgenius

import com.github.mikephil.charting.data.LineData

// A single class to hold the entire state for the UI
data class InsightsState(
    val lineData: LineData = LineData(),
    val dateLabel: String = "",
    val xAxisLabels: List<String> = emptyList(),
    val summaryList: List<InsightSummaryItem> = emptyList(),
    val yAxisMax: Float = 60f,
    val isWeekView: Boolean = true // To show/hide arrows
)

// The state of the user's filter choices
data class InsightsFilterState(
    val timeRange: TimeRange = TimeRange.WEEK,
    val activityIds: Set<Int> = emptySet()
)

enum class TimeRange {
    WEEK, MONTH, SIX_MONTHS, YEAR
}

// Data for the summary list below the chart
data class InsightSummaryItem(
    val activityName: String,
    val totalDurationFormatted: String,
    val color: String
)