package com.example.roboticsgenius

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import java.util.Calendar

object ReminderManager {

    fun scheduleOrUpdateReminder(context: Context, reminder: Reminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (!checkPermissions(alarmManager, context)) return

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_SHOW_REMINDER // Set a clear action
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_MESSAGE, reminder.message)
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_COLOR, reminder.color)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id, // Use the reminder's DB ID as the request code for uniqueness
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)

        // Only schedule if the reminder is enabled and the trigger time is valid (not 0)
        if (reminder.isEnabled && reminder.nextTriggerTime > 0) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.nextTriggerTime,
                pendingIntent
            )
        }
    }

    fun cancelReminder(context: Context, reminderId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun checkPermissions(alarmManager: AlarmManager, context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(
                    context,
                    "Please grant permission to schedule exact alarms in app settings.",
                    Toast.LENGTH_LONG
                ).show()
                return false
            }
        }
        return true
    }

    // THE FIX: Complete rewrite of scheduling logic to handle all cases correctly.
    fun calculateNextTriggerTime(selectedDateMillis: Long, repeatDays: Int): Long {
        val now = Calendar.getInstance()
        val triggerTime = Calendar.getInstance().apply {
            timeInMillis = selectedDateMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Case 1: One-time reminder
        if (repeatDays == 0) {
            // If the selected time is in the past, it's invalid. Return 0 to prevent scheduling.
            return if (triggerTime.after(now)) triggerTime.timeInMillis else 0
        }

        // Case 2: Repeating reminder
        val dayOfWeekMap = mapOf(
            Calendar.SUNDAY to 1, Calendar.MONDAY to 2, Calendar.TUESDAY to 4,
            Calendar.WEDNESDAY to 8, Calendar.THURSDAY to 16, Calendar.FRIDAY to 32, Calendar.SATURDAY to 64
        )

        // First, check if the originally selected date is a valid trigger time
        val initialDayBit = dayOfWeekMap[triggerTime.get(Calendar.DAY_OF_WEEK)] ?: 0
        if ((repeatDays and initialDayBit) != 0 && triggerTime.after(now)) {
            return triggerTime.timeInMillis
        }

        // If not, find the next valid day starting from tomorrow
        val checkDate = Calendar.getInstance().apply {
            timeInMillis = triggerTime.timeInMillis
            add(Calendar.DAY_OF_YEAR, 1) // Start check from the next day
            set(Calendar.HOUR_OF_DAY, triggerTime.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, triggerTime.get(Calendar.MINUTE))
        }

        for (i in 0..7) {
            val dayBit = dayOfWeekMap[checkDate.get(Calendar.DAY_OF_WEEK)] ?: 0
            if ((repeatDays and dayBit) != 0) {
                // Found the next valid day
                return checkDate.timeInMillis
            }
            checkDate.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Fallback if no valid day is found in the next week (should not happen)
        return 0
    }
}