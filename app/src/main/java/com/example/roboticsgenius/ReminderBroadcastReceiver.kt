package com.example.roboticsgenius

import android.Manifest
import android.app.AlarmManager // THE FIX: ADDED MISSING IMPORT
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_REMINDER_ID = "EXTRA_REMINDER_ID"
        const val EXTRA_REMINDER_MESSAGE = "EXTRA_REMINDER_MESSAGE"
        const val EXTRA_REMINDER_COLOR = "EXTRA_REMINDER_COLOR"
        const val REMINDER_CHANNEL_ID = "REMINDER_CHANNEL"

        const val ACTION_SHOW_REMINDER = "ACTION_SHOW_REMINDER"
        const val ACTION_SNOOZE = "ACTION_SNOOZE"
        const val ACTION_SHOW_SNOOZED_REMINDER = "ACTION_SHOW_SNOOZED_REMINDER"
        const val EXTRA_SNOOZE_MINUTES = "EXTRA_SNOOZE_MINUTES"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, -1)
        if (reminderId == -1) return

        val message = intent.getStringExtra(EXTRA_REMINDER_MESSAGE) ?: "Reminder"
        val colorHex = intent.getStringExtra(EXTRA_REMINDER_COLOR) ?: "#808080"

        when (intent.action) {
            ACTION_SHOW_REMINDER -> {
                // This is a main reminder.
                // 1. Immediately reschedule the next one if it repeats.
                rescheduleRepeatingReminder(context, reminderId)
                // 2. Show the notification with snooze buttons.
                showNotification(context, reminderId, message, colorHex, withSnoozeActions = true)
            }
            ACTION_SNOOZE -> {
                val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 5)
                // Schedule a temporary, one-off alarm.
                scheduleSnooze(context, reminderId, message, colorHex, snoozeMinutes)
                // Cancel the original notification that was just snoozed.
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(reminderId)
            }
            ACTION_SHOW_SNOOZED_REMINDER -> {
                // This is the temporary snoozed alarm firing.
                // Show the notification again, but this time WITHOUT snooze buttons.
                showNotification(context, reminderId, message, colorHex, withSnoozeActions = false)
            }
        }
    }

    private fun showNotification(context: Context, reminderId: Int, message: String, colorHex: String, withSnoozeActions: Boolean) {
        val color = try { Color.parseColor(colorHex) } catch (e: Exception) { Color.GRAY }
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingContentIntent = PendingIntent.getActivity(
            context,
            reminderId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_reminder)
            .setContentTitle("Reminder")
            .setContentText(message)
            .setColor(color)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingContentIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        if (withSnoozeActions) {
            builder.addAction(0, "+5 min", createSnoozeIntent(context, reminderId, message, colorHex, 5))
            builder.addAction(0, "+10 min", createSnoozeIntent(context, reminderId, message, colorHex, 10))
            builder.addAction(0, "+1 hr", createSnoozeIntent(context, reminderId, message, colorHex, 60))
            builder.addAction(0, "+1 day", createSnoozeIntent(context, reminderId, message, colorHex, 24 * 60))
        }

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            // Use the original reminderId for the main notification, and a negative ID for the snoozed one.
            val notificationId = if (withSnoozeActions) reminderId else -reminderId
            notify(notificationId, builder.build())
        }
    }

    private fun createSnoozeIntent(context: Context, reminderId: Int, message: String, colorHex: String, minutes: Int): PendingIntent {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_SNOOZE_MINUTES, minutes)
            // Pass the details along so the snooze action can create the final notification.
            putExtra(EXTRA_REMINDER_MESSAGE, message)
            putExtra(EXTRA_REMINDER_COLOR, colorHex)
        }
        val requestCode = reminderId * 100 + minutes
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleSnooze(context: Context, originalId: Int, message: String, colorHex: String, minutes: Int) {
        val newTriggerTime = Calendar.getInstance().timeInMillis + (minutes * 60 * 1000L)
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_SHOW_SNOOZED_REMINDER
            putExtra(EXTRA_REMINDER_ID, originalId)
            putExtra(EXTRA_REMINDER_MESSAGE, message)
            putExtra(EXTRA_REMINDER_COLOR, colorHex)
        }
        // Use a negative ID for the snooze pending intent to ensure it's unique from the main one.
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            -originalId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, newTriggerTime, pendingIntent)
    }


    private fun rescheduleRepeatingReminder(context: Context, reminderId: Int) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val db = AppDatabase.getDatabase(context)
            val reminder = db.reminderDao().getReminderById(reminderId) ?: return@launch

            if (reminder.repeatDays > 0) {
                val nextTriggerTime = calculateNextRepeatingTrigger(reminder.nextTriggerTime, reminder.repeatDays)
                val updatedReminder = reminder.copy(nextTriggerTime = nextTriggerTime)
                db.reminderDao().update(updatedReminder)
                // Schedule the *next* repeating alarm.
                ReminderManager.scheduleOrUpdateReminder(context, updatedReminder)
            }
        }
    }

    private fun calculateNextRepeatingTrigger(lastTriggerTime: Long, repeatDays: Int): Long {
        val nextTrigger = Calendar.getInstance().apply { timeInMillis = lastTriggerTime }
        val dayOfWeekMap = mapOf(
            Calendar.SUNDAY to 1, Calendar.MONDAY to 2, Calendar.TUESDAY to 4,
            Calendar.WEDNESDAY to 8, Calendar.THURSDAY to 16, Calendar.FRIDAY to 32, Calendar.SATURDAY to 64
        )
        // Start checking from the day after the one that just triggered.
        nextTrigger.add(Calendar.DAY_OF_YEAR, 1)

        for (i in 0..7) {
            val dayBit = dayOfWeekMap[nextTrigger.get(Calendar.DAY_OF_WEEK)] ?: 0
            if ((repeatDays and dayBit) != 0) {
                return nextTrigger.timeInMillis
            }
            nextTrigger.add(Calendar.DAY_OF_YEAR, 1)
        }
        return 0 // Should not happen, but as a fallback, indicates no further alarms.
    }
}