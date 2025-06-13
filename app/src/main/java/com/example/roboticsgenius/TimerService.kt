package com.example.roboticsgenius

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask
import kotlin.math.roundToInt

class TimerService : Service() {

    private val timer = Timer()
    private var timeElapsed = 0.0
    private lateinit var notificationManager: NotificationManager
    private lateinit var mediaSession: MediaSessionCompat

    // --- NEW: For saving to database ---
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: AppDatabase
    private var activeActivityId: Int = -1
    private var startTime: Long = 0
    // --- END NEW ---

    companion object {
        var isRunning = false
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, "RoboticsGeniusMediaSession")
        db = AppDatabase.getDatabase(this) // Initialize database
    }

    override fun onBind(intent: Intent?): IBinder? { return null }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            saveTimeLogAndStop()
            return START_NOT_STICKY
        }
        if (isRunning) {
            Log.d("TimerService", "Service is already running. Ignoring.")
            return START_STICKY
        }

        // --- Get the activity ID from the intent ---
        activeActivityId = intent?.getIntExtra("ACTIVITY_ID", -1) ?: -1
        if (activeActivityId == -1) {
            // If no valid ID, stop immediately.
            stopSelf()
            return START_NOT_STICKY
        }
        startTime = System.currentTimeMillis()
        // --- END ---

        Log.d("TimerService", "Service starting for activity ID: $activeActivityId")
        isRunning = true
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                timeElapsed++
                updateNotification()
            }
        }, 0, 1000)

        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val pStopSelf = PendingIntent.getService(this, 0,
            Intent(this, TimerService::class.java).apply { action = ACTION_STOP_SERVICE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificationBuilder = NotificationCompat.Builder(this, "TIMER_SERVICE_CHANNEL")
            .setContentTitle("Timer Active")
            .setContentText(formatTime(timeElapsed))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true).setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop", pStopSelf)
        notificationBuilder.setStyle(MediaNotificationCompat.MediaStyle()
            .setMediaSession(mediaSession.sessionToken).setShowActionsInCompactView(0)
        )
        return notificationBuilder.build()
    }

    private fun updateNotification() { notificationManager.notify(NOTIFICATION_ID, createNotification()) }
    private fun formatTime(s: Double): String {
        val r = s.roundToInt(); return String.format("%02d:%02d:%02d", r / 3600, (r % 3600) / 60, r % 60)
    }

    private fun saveTimeLogAndStop() {
        serviceScope.launch {
            val logEntry = TimeLogEntry(
                activityId = activeActivityId,
                startTime = startTime,
                durationInSeconds = timeElapsed.roundToInt()
            )
            db.activityDao().insertTimeLog(logEntry)
            Log.d("TimerService", "SAVED LOG: $logEntry")
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        isRunning = false
        timer.cancel()
        mediaSession.release()
        super.onDestroy()
        Log.d("TimerService", "Service is being destroyed.")
    }
}