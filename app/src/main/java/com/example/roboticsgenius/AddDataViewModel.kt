package com.example.roboticsgenius

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddDataViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val activityDao = db.activityDao()
    private val dataEntryDao = db.dataEntryDao()

    private val _dataSetId = MutableStateFlow(-1)
    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    val selectedDate = _selectedDate.asStateFlow()

    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val isPreviousEnabled: StateFlow<Boolean> = _selectedDate.map {
        !isSameDay(it, SettingsManager.getAppStartDateCalendar())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isNextEnabled: StateFlow<Boolean> = _selectedDate.map {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        !isSameDay(it, today)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val uiState: Flow<List<DataActivityUiModel>> = combine(
        _dataSetId,
        _selectedDate
    ) { dataSetId, date ->
        Pair(dataSetId, date)
    }.flatMapLatest { (dataSetId, date) ->
        if (dataSetId == -1) {
            return@flatMapLatest flowOf(emptyList())
        }
        val dateString = dbDateFormat.format(date.time)

        combine(
            activityDao.getActivitiesForDataSet(dataSetId),
            dataEntryDao.getEntriesForDate(dateString)
        ) { activities, entries ->
            activities.map { activity ->
                DataActivityUiModel(
                    activity = activity,
                    entry = entries.find { it.activityId == activity.id }
                )
            }
        }
    }

    fun upsertDataEntry(activityId: Int, value: String, existingEntryId: Int?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val dateString = dbDateFormat.format(_selectedDate.value.time)
            val entry = DataEntry(
                id = existingEntryId ?: 0,
                activityId = activityId,
                date = dateString,
                value = value
            )
            dataEntryDao.upsert(entry)
            onSuccess()
        }
    }

    suspend fun getSuggestions(activityId: Int): List<String> {
        val endDate = Calendar.getInstance()
        val startDate = (endDate.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -15)
        }
        return dataEntryDao.getMostFrequentValues(
            activityId,
            dbDateFormat.format(startDate.time),
            dbDateFormat.format(endDate.time)
        )
    }

    fun saveDataActivityOrder(reorderedActivities: List<DataActivityUiModel>) {
        viewModelScope.launch {
            val updatedActivities = reorderedActivities.mapIndexed { index, uiModel ->
                uiModel.activity.copy(orderIndex = index)
            }
            activityDao.updateActivities(updatedActivities)
        }
    }

    fun setDataSetId(id: Int) {
        _dataSetId.value = id
    }

    fun changeDate(amount: Int) {
        _selectedDate.value = (_selectedDate.value.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, amount)
        }
    }

    fun setDate(year: Int, month: Int, dayOfMonth: Int) {
        _selectedDate.value = (_selectedDate.value.clone() as Calendar).apply {
            set(year, month, dayOfMonth)
        }
    }

    fun createDataActivity(activity: Activity) {
        viewModelScope.launch {
            val maxIndex = activityDao.getMaxOrderIndex() ?: -1
            val newActivity = activity.copy(orderIndex = maxIndex + 1)
            activityDao.insert(newActivity)
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}