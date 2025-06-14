// app/src/main/java/com/example/roboticsgenius/RoboticsGeniusApp.kt

package com.example.roboticsgenius
import android.app.*
import android.content.Context
import android.os.Build

class RoboticsGeniusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // THE FIX IS HERE: Changed IMPORTANCE_DEFAULT to IMPORTANCE_LOW
            val channel = NotificationChannel(
                "TIMER_SERVICE_CHANNEL",
                "Timer Service Channel",
                NotificationManager.IMPORTANCE_LOW // This makes notifications silent
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}