package com.example.roboticsgenius

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class RemindersViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val reminderDao = db.reminderDao()

    val activeReminders: StateFlow<List<Reminder>> = reminderDao.getAllReminders()
        .map { reminders ->
            val today = Calendar.getInstance()
            val startOfToday = (today.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            reminders.filter {
                if (it.repeatDays > 0) {
                    true
                } else {
                    val reminderCal = Calendar.getInstance().apply { timeInMillis = it.nextTriggerTime }
                    val reminderStartOfDay = (reminderCal.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    // Show non-repeating reminders if their trigger time is valid (not 0)
                    // and their day is today or in the future.
                    it.nextTriggerTime > 0 && reminderStartOfDay >= startOfToday
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun upsertReminder(reminder: Reminder) {
        viewModelScope.launch {
            // THE FIX: Ensure we get the ID back from the upsert before scheduling
            val newId = reminderDao.upsert(reminder)
            // If it was a new reminder, its ID was 0. Use the newId returned from the DB.
            val finalReminder = if (reminder.id == 0) reminder.copy(id = newId.toInt()) else reminder
            ReminderManager.scheduleOrUpdateReminder(getApplication(), finalReminder)
        }
    }


    fun deleteReminder(reminderId: Int) {
        viewModelScope.launch {
            ReminderManager.cancelReminder(getApplication(), reminderId)
            reminderDao.deleteById(reminderId)
        }
    }

    suspend fun getReminderById(reminderId: Int): Reminder? {
        return reminderDao.getReminderById(reminderId)
    }
}