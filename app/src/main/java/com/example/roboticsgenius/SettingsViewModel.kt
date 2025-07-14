package com.example.roboticsgenius

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dataSetDao = db.dataSetDao()
    private val activityDao = db.activityDao()

    val appStartDate: StateFlow<Calendar> = SettingsManager.appStartDate

    val deletionItems: StateFlow<List<DeletionItem>> =
        // THE FIX: Use the new DAO function to get all activities, not just TimeTracker ones.
        combine(dataSetDao.getAllDataSets(), activityDao.getAllActivitiesForSettings()) { dataSets, allActivities ->
            val items = mutableListOf<DeletionItem>()
            dataSets.forEach { dataSet ->
                items.add(DeletionItem.DataSetItem(dataSet))
                val activitiesInData = allActivities.filter { it.dataSetId == dataSet.id }
                activitiesInData.forEach { activity ->
                    items.add(DeletionItem.ActivityItem(activity))
                }
            }
            items
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setAppStartDate(calendar: Calendar) {
        SettingsManager.setAppStartDate(calendar)
    }

    fun deleteDataSet(dataSet: DataSet) {
        viewModelScope.launch {
            dataSetDao.deleteDataSetAndChildren(dataSet.id)
        }
    }

    fun deleteActivity(activity: Activity) {
        viewModelScope.launch {
            activityDao.deleteActivityAndLogs(activity)
        }
    }
}

sealed class DeletionItem {
    data class DataSetItem(val dataSet: DataSet) : DeletionItem()
    data class ActivityItem(val activity: Activity) : DeletionItem()
}