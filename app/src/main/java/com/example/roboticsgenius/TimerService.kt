// TimerService.kt
package com.example.roboticsgenius
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import androidx.media.app.NotificationCompat as MediaNotificationCompat

class TimerService : Service() {
    private lateinit var timer: Timer
    private lateinit var notificationManager: NotificationManager
    private lateinit var mediaSession: MediaSessionCompat
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: AppDatabase
    private var startTime: Long = 0

    companion object {
        private val _timeElapsed = MutableStateFlow(0)
        val timeElapsed = _timeElapsed.asStateFlow()
        private val _activeActivityId = MutableStateFlow<Int?>(null)
        val activeActivityId = _activeActivityId.asStateFlow()
        const val ACTION_START = "ACTION_START"; const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_ACTIVITY_ID = "EXTRA_ACTIVITY_ID"; const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, "RoboticsGeniusMediaSession")
        db = AppDatabase.getDatabase(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> if (activeActivityId.value == null) intent.getIntExtra(EXTRA_ACTIVITY_ID, -1).takeIf { it != -1 }?.let { startTimer(it) }
            ACTION_STOP -> stopTimer()
        }
        return START_STICKY
    }

    private fun startTimer(activityId: Int) {
        _activeActivityId.value = activityId; _timeElapsed.value = 0; startTime = System.currentTimeMillis()
        timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() { _timeElapsed.value++; updateNotification() }
        }, 0, 1000)
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun stopTimer() {
        if (activeActivityId.value == null) return
        if(this::timer.isInitialized) timer.cancel()
        val idToSave = activeActivityId.value!!; val timeToSave = _timeElapsed.value
        serviceScope.launch { db.activityDao().insertTimeLog(TimeLogEntry(activityId = idToSave, startTime = startTime, durationInSeconds = timeToSave)) }
        _activeActivityId.value = null; _timeElapsed.value = 0
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }

    override fun onDestroy() { super.onDestroy(); mediaSession.release() }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun createNotification(): Notification {
        val pStop = PendingIntent.getService(this, 0, Intent(this, TimerService::class.java).apply { action = ACTION_STOP }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(this, "TIMER_SERVICE_CHANNEL").setContentTitle("Timer Active").setContentText(formatTime(_timeElapsed.value)).setSmallIcon(android.R.drawable.ic_dialog_info).setOngoing(true).setOnlyAlertOnce(true).addAction(android.R.drawable.ic_media_pause, "Stop", pStop)
        builder.setStyle(MediaNotificationCompat.MediaStyle().setMediaSession(mediaSession.sessionToken).setShowActionsInCompactView(0))
        return builder.build()
    }
    private fun updateNotification() { notificationManager.notify(NOTIFICATION_ID, createNotification()) }
    private fun formatTime(seconds: Int) = String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60)
}