// app/src/main/java/com/example/roboticsgenius/RoboticsGeniusApp.kt

package com.example.roboticsgenius
import android.app.*
import android.content.Context
import android.os.Build

class RoboticsGeniusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Timer Service Channel (Low importance)
            val timerChannel = NotificationChannel(
                "TIMER_SERVICE_CHANNEL",
                "Timer Service Channel",
                NotificationManager.IMPORTANCE_LOW // This makes notifications silent
            )

            // Reminder Channel (High importance)
            val reminderChannel = NotificationChannel(
                ReminderBroadcastReceiver.REMINDER_CHANNEL_ID,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows reminders set by the user."
            }


            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(timerChannel)
            manager.createNotificationChannel(reminderChannel)
        }
    }
}