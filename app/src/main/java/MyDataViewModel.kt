// app/src/main/java/com/example/roboticsgenius/MyDataViewModel.kt
package com.example.roboticsgenius

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class MyDataTimePeriod { WEEKLY, MONTHLY, YEARLY, CUSTOM }

data class MyDataSettings(
    val timePeriod: MyDataTimePeriod = MyDataTimePeriod.MONTHLY,
    val customStartDate: Long = System.currentTimeMillis(),
    val customEndDate: Long = System.currentTimeMillis()
)

data class MyDataUiState(
    val isLoading: Boolean = true,
    val showEmptyState: Boolean = false,
    val selectedDataSet: DataSet? = null,
    val dateNavigatorLabel: String = "",
    val showDateNavigatorArrows: Boolean = true,
    val dateLabels: List<String> = emptyList(),
    val activities: List<Activity> = emptyList(),
    val dataGrid: List<List<String>> = emptyList()
)

class MyDataViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dataSetDao = db.dataSetDao()
    private val activityDao = db.activityDao()
    private val dataEntryDao = db.dataEntryDao()

    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val shortDisplayFormat = SimpleDateFormat("d MMM", Locale.US)
    private val monthDisplayFormat = SimpleDateFormat("MMMM", Locale.US)
    private val yearDisplayFormat = SimpleDateFormat("yyyy", Locale.US)

    private val _selectedDataSetId = MutableStateFlow<Int?>(null)
    val allDataSets: StateFlow<List<DataSet>> = dataSetDao.getAllDataSets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _settings = MutableStateFlow(MyDataSettings())
    val settings: StateFlow<MyDataSettings> = _settings

    private val _viewDate = MutableStateFlow(Calendar.getInstance())

    val uiState: StateFlow<MyDataUiState> = combine(
        _selectedDataSetId, _settings, _viewDate
    ) { id, settings, viewDate -> Triple(id, settings, viewDate) }
        .flatMapLatest { (id, settings, viewDate) ->
            if (id == null) {
                return@flatMapLatest flowOf(MyDataUiState(isLoading = true, showEmptyState = allDataSets.value.isEmpty()))
            }

            val (startCal, endCal) = calculateDateRange(settings, viewDate)
            val startDateStr = dbDateFormat.format(startCal.time)
            val endDateStr = dbDateFormat.format(endCal.time)

            // THE FIX IS HERE: `allDataSets` is now a source flow within the combine block.
            // This resolves all the type inference errors.
            combine(
                allDataSets, // Flow 1: Provides the list of all datasets
                activityDao.getAllActivitiesForDataSet(id), // Flow 2
                dataEntryDao.getEntriesInDateRange(startDateStr, endDateStr), // Flow 3
                activityDao.getAllTimeLogs() // Flow 4
            ) { dataSetList, activities, dataEntries, timeLogs ->
                // We find the selectedDataSet from the full list that Flow 1 provides.
                val selectedDataSet = dataSetList.find { it.id == id }
                // Then, we pass all the resolved data to the processing function.
                processData(selectedDataSet, activities, dataEntries, timeLogs, startCal, endCal, settings)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MyDataUiState(isLoading = true))

    private fun processData(
        selectedDataSet: DataSet?,
        activities: List<Activity>,
        dataEntries: List<DataEntry>,
        timeLogs: List<TimeLogEntry>,
        startCal: Calendar,
        endCal: Calendar,
        settings: MyDataSettings
    ): MyDataUiState {
        val datesInRange = getDatesBetween(startCal, endCal).asReversed()
        val dateLabels = datesInRange.map { date -> shortDisplayFormat.format(date.time) }

        val timeLogsByDateAndActivity = timeLogs
            .filter { log -> log.startTime >= startCal.timeInMillis && log.startTime <= endCal.timeInMillis }
            .groupBy { log -> dbDateFormat.format(Date(log.startTime)) }
            .mapValues { entry -> entry.value.groupBy { log -> log.activityId } }

        val dataEntriesByDateAndActivity = dataEntries
            .groupBy { entry -> entry.date }
            .mapValues { entry -> entry.value.associateBy { data -> data.activityId } }

        val dataGrid = datesInRange.map { date ->
            val dateString = dbDateFormat.format(date.time)
            activities.map { activity ->
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

        return MyDataUiState(
            isLoading = false,
            showEmptyState = false,
            selectedDataSet = selectedDataSet,
            dateNavigatorLabel = generateDateNavigatorLabel(startCal, endCal, settings.timePeriod),
            showDateNavigatorArrows = settings.timePeriod != MyDataTimePeriod.CUSTOM,
            dateLabels = dateLabels,
            activities = activities,
            dataGrid = dataGrid
        )
    }

    private fun calculateDateRange(settings: MyDataSettings, viewDate: Calendar): Pair<Calendar, Calendar> {
        val startCal = viewDate.clone() as Calendar
        val endCal = viewDate.clone() as Calendar

        when (settings.timePeriod) {
            MyDataTimePeriod.WEEKLY -> {
                startCal.firstDayOfWeek = Calendar.MONDAY
                startCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                endCal.timeInMillis = startCal.timeInMillis
                endCal.add(Calendar.DAY_OF_YEAR, 6)
            }
            MyDataTimePeriod.MONTHLY -> {
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            MyDataTimePeriod.YEARLY -> {
                startCal.set(Calendar.DAY_OF_YEAR, 1)
                endCal.set(Calendar.DAY_OF_YEAR, endCal.getActualMaximum(Calendar.DAY_OF_YEAR))
            }
            MyDataTimePeriod.CUSTOM -> {
                startCal.timeInMillis = settings.customStartDate
                endCal.timeInMillis = settings.customEndDate
            }
        }
        return Pair(startCal, endCal)
    }

    fun applySettings(newSettings: MyDataSettings) {
        _viewDate.value = Calendar.getInstance() // Reset to today when settings change
        _settings.value = newSettings
    }

    fun navigateDate(direction: Int) {
        val increment = when (_settings.value.timePeriod) {
            MyDataTimePeriod.WEEKLY -> Calendar.WEEK_OF_YEAR
            MyDataTimePeriod.MONTHLY -> Calendar.MONTH
            MyDataTimePeriod.YEARLY -> Calendar.YEAR
            else -> return // Do not navigate for custom
        }
        _viewDate.value = (_viewDate.value.clone() as Calendar).apply {
            add(increment, direction)
        }
    }

    fun updateActivityOrder(activities: List<Activity>) {
        viewModelScope.launch {
            val updatedActivities = activities.mapIndexed { index, activity ->
                activity.copy(orderIndex = index)
            }
            activityDao.updateActivities(updatedActivities)
        }
    }

    fun selectDataSet(id: Int) {
        if (_selectedDataSetId.value != id) {
            _selectedDataSetId.value = id
        }
    }

    private fun generateDateNavigatorLabel(start: Calendar, end: Calendar, period: MyDataTimePeriod): String {
        return when (period) {
            MyDataTimePeriod.WEEKLY -> "${shortDisplayFormat.format(start.time)} - ${shortDisplayFormat.format(end.time)}"
            MyDataTimePeriod.MONTHLY -> monthDisplayFormat.format(start.time)
            MyDataTimePeriod.YEARLY -> yearDisplayFormat.format(start.time)
            MyDataTimePeriod.CUSTOM -> "${shortDisplayFormat.format(start.time)} - ${shortDisplayFormat.format(end.time)}"
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