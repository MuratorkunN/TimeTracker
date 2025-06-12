package com.example.roboticsgenius

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class TimerService : Service() {

    companion object {
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // --- Check if the user clicked the "Stop" button ---
        if (intent?.action == ACTION_STOP_SERVICE) {
            Log.d("TimerService", "Stopping service now...")
            stopSelf()
            return START_NOT_STICKY
        }

        Log.d("TimerService", "Service is starting...")

        // --- Build the persistent notification ---
        val stopSelfIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val pStopSelf = PendingIntent.getService(this, 0, stopSelfIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification: Notification = NotificationCompat.Builder(this, "TIMER_SERVICE_CHANNEL")
            .setContentTitle("Timer Active")
            .setContentText("00:00:00") // We will make this update later
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use the default app icon
            .addAction(android.R.drawable.ic_media_pause, "Stop", pStopSelf)
            .setOngoing(true) // Makes it non-swipeable
            .build()

        // --- THIS IS THE MAGIC LINE ---
        // It starts the service in the foreground and shows the notification.
        startForeground(1, notification)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("TimerService", "Service is being destroyed.")
    }
}