package com.example.roboticsgenius

import com.github.mikephil.charting.data.LineData

data class InsightsState(
    val lineData: LineData = LineData(),
    val dateLabel: String = "",
    val xAxisLabels: List<String> = emptyList(),
    val summaryList: List<InsightSummaryItem> = emptyList(),
    val yAxisMax: Float = 60f,
    val isWeekView: Boolean = true
)

data class InsightsFilterState(
    val timeRange: TimeRange = TimeRange.WEEK,
    val activityIds: Set<Int> = emptySet()
)

enum class TimeRange {
    // THE FIX: Removed SIX_MONTHS
    WEEK, MONTH, YEAR
}

data class InsightSummaryItem(
    val activityName: String,
    val totalDurationFormatted: String,
    val color: String
)