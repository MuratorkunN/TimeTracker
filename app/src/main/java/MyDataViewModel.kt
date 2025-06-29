// app/src/main/java/com/example/roboticsgenius/MyDataViewModel.kt
package com.example.roboticsgenius

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// A state class to hold all the processed data for the UI
data class MyDataUiState(
    val isLoading: Boolean = true,
    val showEmptyState: Boolean = false,
    val selectedDataSet: DataSet? = null,
    val dateLabels: List<String> = emptyList(),
    val activities: List<Activity> = emptyList(),
    val dataGrid: List<List<String>> = emptyList() // 2D list: List of rows, where each row is a list of cell strings
)

class MyDataViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dataSetDao = db.dataSetDao()
    private val activityDao = db.activityDao()
    private val dataEntryDao = db.dataEntryDao()

    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormat = SimpleDateFormat("MMM d", Locale.US)

    // Holds the ID of the dataset currently being viewed
    private val _selectedDataSetId = MutableStateFlow<Int?>(null)

    // Public StateFlow to expose all available datasets for the bottom navigation
    val allDataSets: StateFlow<List<DataSet>> = dataSetDao.getAllDataSets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // The main StateFlow that combines all data sources to build the final UI state
    val uiState: StateFlow<MyDataUiState> = _selectedDataSetId.flatMapLatest { id ->
        if (id == null) {
            // If no dataset is selected, show loading or empty state
            return@flatMapLatest flowOf(MyDataUiState(isLoading = true, showEmptyState = allDataSets.value.isEmpty()))
        }

        // Define the date range (for now, the current month)
        // TODO: This will be configurable from the settings dialog later
        val endCal = Calendar.getInstance()
        val startCal = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }

        val startDateStr = dbDateFormat.format(startCal.time)
        val endDateStr = dbDateFormat.format(endCal.time)

        val selectedDataSetFlow = allDataSets.map { list -> list.find { dataSet -> dataSet.id == id } }

        // Combine all necessary data flows
        combine(
            selectedDataSetFlow,
            activityDao.getAllActivitiesForDataSet(id), // Gets ALL activities in the set
            dataEntryDao.getEntriesInDateRange(startDateStr, endDateStr), // THIS NOW WORKS
            activityDao.getAllTimeLogs()
        ) { selectedDataSet, activities, dataEntries, timeLogs ->

            // --- DATA PROCESSING ---
            val datesInRange = getDatesBetween(startCal, endCal)
            val dateLabels = datesInRange.map { date -> displayDateFormat.format(date.time) }

            // Create maps for quick lookup
            val timeLogsByDateAndActivity = timeLogs
                .filter { log -> log.startTime >= startCal.timeInMillis && log.startTime <= endCal.timeInMillis }
                .groupBy { log -> dbDateFormat.format(Date(log.startTime)) }
                .mapValues { entry ->
                    entry.value.groupBy { log -> log.activityId }
                }

            val dataEntriesByDateAndActivity = dataEntries
                .groupBy { entry -> entry.date }
                .mapValues { entry ->
                    entry.value.associateBy { data -> data.activityId }
                }

            val dataGrid = datesInRange.map { date ->
                val dateString = dbDateFormat.format(date.time)
                activities.map { activity ->
                    // For each cell in the grid, find its value
                    if (activity.isTimeTrackerActivity) {
                        val totalSeconds = timeLogsByDateAndActivity[dateString]?.get(activity.id)
                            ?.sumOf { log -> log.durationInSeconds } ?: 0
                        formatDuration(totalSeconds)
                    } else {
                        val value = dataEntriesByDateAndActivity[dateString]?.get(activity.id)?.value
                        formatDataEntry(value, activity)
                    }
                }
            }

            // --- EMIT FINAL STATE ---
            MyDataUiState(
                isLoading = false,
                showEmptyState = false,
                selectedDataSet = selectedDataSet,
                dateLabels = dateLabels,
                activities = activities,
                dataGrid = dataGrid
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MyDataUiState(isLoading = true)
    )

    fun selectDataSet(id: Int) {
        if (_selectedDataSetId.value != id) {
            _selectedDataSetId.value = id
        }
    }

    private fun getDatesBetween(startDate: Calendar, endDate: Calendar): List<Calendar> {
        val dates = mutableListOf<Calendar>()
        val current = startDate.clone() as Calendar
        while (!current.after(endDate)) {
            dates.add(current.clone() as Calendar)
            current.add(Calendar.DATE, 1)
        }
        return dates
    }

    private fun formatDuration(totalSeconds: Int): String {
        if (totalSeconds <= 0) return "-"
        val hours = TimeUnit.SECONDS.toHours(totalSeconds.toLong())
        val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds.toLong()) % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }

    private fun formatDataEntry(value: String?, activity: Activity): String {
        if (value.isNullOrBlank()) return "-"
        return when (activity.dataType) {
            "Checkbox" -> if (value.toBoolean()) "✓" else "✗"
            else -> value
        }
    }
}