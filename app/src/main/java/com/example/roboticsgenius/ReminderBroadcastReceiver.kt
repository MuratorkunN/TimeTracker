package com.example.roboticsgenius

import android.Manifest
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

        // THE FIX: Define actions for the notification buttons
        const val ACTION_SHOW_REMINDER = "ACTION_SHOW_REMINDER"
        const val ACTION_SNOOZE = "ACTION_SNOOZE"
        const val EXTRA_SNOOZE_MINUTES = "EXTRA_SNOOZE_MINUTES"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, -1)
        if (reminderId == -1) return

        when (intent.action) {
            ACTION_SHOW_REMINDER -> {
                val message = intent.getStringExtra(EXTRA_REMINDER_MESSAGE) ?: "Reminder"
                val colorHex = intent.getStringExtra(EXTRA_REMINDER_COLOR) ?: "#808080"
                showNotification(context, reminderId, message, colorHex)
                rescheduleIfNeeded(context, reminderId)
            }
            // THE FIX: Handle snooze actions
            ACTION_SNOOZE -> {
                val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 5)
                snoozeReminder(context, reminderId, snoozeMinutes)
                // Dismiss the original notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(reminderId)
            }
        }
    }

    private fun showNotification(context: Context, reminderId: Int, message: String, colorHex: String) {
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

        // THE FIX: Create pending intents for snooze actions
        val snooze5mIntent = createSnoozeIntent(context, reminderId, 5)
        val snooze10mIntent = createSnoozeIntent(context, reminderId, 10)
        val snooze1hrIntent = createSnoozeIntent(context, reminderId, 60)
        val snooze1dayIntent = createSnoozeIntent(context, reminderId, 24 * 60)

        val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_reminder)
            .setContentTitle("Reminder")
            .setContentText(message)
            .setColor(color)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingContentIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .addAction(0, "+5 min", snooze5mIntent)
            .addAction(0, "+10 min", snooze10mIntent)
            .addAction(0, "+1 hr", snooze1hrIntent)
            .addAction(0, "+1 day", snooze1dayIntent)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            notify(reminderId, builder.build())
        }
    }

    private fun createSnoozeIntent(context: Context, reminderId: Int, minutes: Int): PendingIntent {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_SNOOZE_MINUTES, minutes)
        }
        // Use minutes as part of the request code to ensure uniqueness for each button
        val requestCode = reminderId * 100 + minutes
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun snoozeReminder(context: Context, reminderId: Int, minutes: Int) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val db = AppDatabase.getDatabase(context)
            val reminder = db.reminderDao().getReminderById(reminderId) ?: return@launch

            val newTriggerTime = Calendar.getInstance().timeInMillis + (minutes * 60 * 1000L)
            val updatedReminder = reminder.copy(nextTriggerTime = newTriggerTime)

            db.reminderDao().update(updatedReminder)
            ReminderManager.scheduleOrUpdateReminder(context, updatedReminder)
        }
    }

    private fun rescheduleIfNeeded(context: Context, reminderId: Int) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val db = AppDatabase.getDatabase(context)
            val reminder = db.reminderDao().getReminderById(reminderId) ?: return@launch

            if (reminder.repeatDays > 0) {
                val nextTriggerTime = calculateNextRepeatingTrigger(reminder.nextTriggerTime, reminder.repeatDays)
                val updatedReminder = reminder.copy(nextTriggerTime = nextTriggerTime)
                db.reminderDao().update(updatedReminder)
                ReminderManager.scheduleOrUpdateReminder(context, updatedReminder)
            } else {
                // One-time reminders are not deleted here anymore. They are filtered out in the ViewModel.
            }
        }
    }

    private fun calculateNextRepeatingTrigger(lastTriggerTime: Long, repeatDays: Int): Long {
        val nextTrigger = Calendar.getInstance().apply { timeInMillis = lastTriggerTime }
        val dayOfWeekMap = mapOf(
            Calendar.SUNDAY to 1, Calendar.MONDAY to 2, Calendar.TUESDAY to 4,
            Calendar.WEDNESDAY to 8, Calendar.THURSDAY to 16, Calendar.FRIDAY to 32, Calendar.SATURDAY to 64
        )

        nextTrigger.add(Calendar.DAY_OF_YEAR, 1)

        for (i in 0..7) {
            val dayBit = dayOfWeekMap[nextTrigger.get(Calendar.DAY_OF_WEEK)] ?: 0
            if ((repeatDays and dayBit) != 0) {
                return nextTrigger.timeInMillis
            }
            nextTrigger.add(Calendar.DAY_OF_YEAR, 1)
        }
        return lastTriggerTime + (7 * 24 * 60 * 60 * 1000) // Fallback
    }
}