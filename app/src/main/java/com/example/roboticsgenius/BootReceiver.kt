package com.example.roboticsgenius

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // THE FIX: Also listen for MY_PACKAGE_REPLACED to re-schedule after an app update.
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                val db = AppDatabase.getDatabase(context)
                val reminders = db.reminderDao().getAllReminders().first()
                reminders.forEach { reminder ->
                    if (reminder.isEnabled) {
                        // Re-calculate the next trigger time in case the device was off for a long time
                        val nextTriggerTime = ReminderManager.calculateNextTriggerTime(reminder.nextTriggerTime, reminder.repeatDays)
                        val updatedReminder = reminder.copy(nextTriggerTime = nextTriggerTime)
                        db.reminderDao().update(updatedReminder)
                        ReminderManager.scheduleOrUpdateReminder(context, updatedReminder)
                    }
                }
            }
        }
    }
}