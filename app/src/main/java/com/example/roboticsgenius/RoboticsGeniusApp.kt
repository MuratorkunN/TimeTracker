package com.example.roboticsgenius
import android.app.*
import android.content.Context
import android.os.Build

class RoboticsGeniusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize SettingsManager on app start.
        SettingsManager.init(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timerChannel = NotificationChannel(
                "TIMER_SERVICE_CHANNEL",
                "Timer Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
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