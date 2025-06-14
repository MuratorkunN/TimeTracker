// RoboticsGeniusApp.kt
package com.example.roboticsgenius
import android.app.*
import android.content.Context
import android.os.Build
class RoboticsGeniusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("TIMER_SERVICE_CHANNEL", "Timer Service Channel", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}