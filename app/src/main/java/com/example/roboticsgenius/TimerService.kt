// app/src/main/java/com/example/roboticsgenius/TimerService.kt

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
    private var timerJob: Job? = null
    private lateinit var notificationManager: NotificationManager
    private lateinit var mediaSession: MediaSessionCompat
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: AppDatabase
    private var startTime: Long = 0

    companion object {
        // Public state flows for UI observation
        private val _timeElapsed = MutableStateFlow(0)
        val timeElapsed = _timeElapsed.asStateFlow()

        private val _activeActivityId = MutableStateFlow<Int?>(null)
        val activeActivityId = _activeActivityId.asStateFlow()

        private val _isPaused = MutableStateFlow(false)
        val isPaused = _isPaused.asStateFlow()

        private val _activeActivityName = MutableStateFlow("Timer")
        val activeActivityName = _activeActivityName.asStateFlow()

        // Service Actions
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_CANCEL = "ACTION_CANCEL"

        // Intent Extras
        const val EXTRA_ACTIVITY_ID = "EXTRA_ACTIVITY_ID"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, "RoboticsGeniusMediaSession")
        db = AppDatabase.getDatabase(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> if (activeActivityId.value == null) {
                intent.getIntExtra(EXTRA_ACTIVITY_ID, -1).takeIf { it != -1 }?.let {
                    startTimer(it)
                }
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_STOP -> stopAndSaveTimer()
            ACTION_CANCEL -> cancelTimer()
        }
        return START_NOT_STICKY // More appropriate for timers that can be cancelled
    }

    private fun startTimer(activityId: Int) {
        serviceScope.launch {
            val activity = db.activityDao().getActivityById(activityId)
            activity?.let {
                withContext(Dispatchers.Main) {
                    _activeActivityName.value = it.name
                    _activeActivityId.value = activityId
                    _timeElapsed.value = 0
                    _isPaused.value = false
                    startTime = System.currentTimeMillis()

                    timerJob = serviceScope.launch {
                        while (true) {
                            if (!_isPaused.value) {
                                _timeElapsed.value++
                                updateNotification()
                            }
                            delay(1000)
                        }
                    }
                    startForeground(NOTIFICATION_ID, createNotification())
                }
            }
        }
    }

    private fun pauseTimer() {
        _isPaused.value = true
        updateNotification()
    }

    private fun resumeTimer() {
        _isPaused.value = false
        updateNotification()
    }

    private fun stopAndSaveTimer() {
        if (activeActivityId.value == null) return
        val idToSave = activeActivityId.value!!
        val timeToSave = _timeElapsed.value
        serviceScope.launch {
            db.activityDao().insertTimeLog(
                TimeLogEntry(
                    activityId = idToSave,
                    startTime = startTime,
                    durationInSeconds = timeToSave
                )
            )
        }
        resetStateAndStopService()
    }

    private fun cancelTimer() {
        // No log is saved
        resetStateAndStopService()
    }

    private fun resetStateAndStopService() {
        timerJob?.cancel()
        _activeActivityId.value = null
        _timeElapsed.value = 0
        _isPaused.value = false
        _activeActivityName.value = "Timer"
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        mediaSession.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        // Cancel Action
        val cancelIntent = Intent(this, TimerService::class.java).apply { action = ACTION_CANCEL }
        val pCancel = PendingIntent.getService(this, 1, cancelIntent, PendingIntent.FLAG_IMMUTABLE)

        // Pause/Resume Action
        val pPauseResume: PendingIntent
        val pauseResumeIcon: Int
        val pauseResumeTitle: String
        if (_isPaused.value) {
            val resumeIntent = Intent(this, TimerService::class.java).apply { action = ACTION_RESUME }
            pPauseResume = PendingIntent.getService(this, 2, resumeIntent, PendingIntent.FLAG_IMMUTABLE)
            pauseResumeIcon = android.R.drawable.ic_media_play
            pauseResumeTitle = "Resume"
        } else {
            val pauseIntent = Intent(this, TimerService::class.java).apply { action = ACTION_PAUSE }
            pPauseResume = PendingIntent.getService(this, 2, pauseIntent, PendingIntent.FLAG_IMMUTABLE)
            pauseResumeIcon = android.R.drawable.ic_media_pause
            pauseResumeTitle = "Pause"
        }

        // Stop Action
        val stopIntent = Intent(this, TimerService::class.java).apply { action = ACTION_STOP }
        val pStop = PendingIntent.getService(this, 3, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, "TIMER_SERVICE_CHANNEL")
            .setContentTitle(_activeActivityName.value)
            .setContentText(formatTime(_timeElapsed.value))
            .setSmallIcon(R.drawable.ic_stat_timer) // A custom icon is better
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", pCancel)
            .addAction(pauseResumeIcon, pauseResumeTitle, pPauseResume)
            .addAction(android.R.drawable.ic_media_next, "Stop", pStop)

        builder.setStyle(
            MediaNotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0, 1, 2) // Show all 3 actions
        )
        return builder.build()
    }

    private fun updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun formatTime(seconds: Int) =
        String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60)
}