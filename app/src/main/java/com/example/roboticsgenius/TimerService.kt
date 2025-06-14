// app/src/main/java/com/example/roboticsgenius/TimerService.kt

package com.example.roboticsgenius

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.Calendar

class TimerService : Service() {
    private var timerJob: Job? = null
    private lateinit var notificationManager: NotificationManager
    private lateinit var mediaSession: MediaSessionCompat
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: AppDatabase
    private var startTimeMillis: Long = 0
    private var currentActivity: Activity? = null
    private var previouslyLoggedSeconds: Int = 0

    companion object {
        private val _timeElapsed = MutableStateFlow(0)
        val timeElapsed = _timeElapsed.asStateFlow()
        private val _activeActivityId = MutableStateFlow<Int?>(null)
        val activeActivityId = _activeActivityId.asStateFlow()
        private val _isPaused = MutableStateFlow(false)
        val isPaused = _isPaused.asStateFlow()
        private val _activeActivityName = MutableStateFlow("Timer")
        val activeActivityName = _activeActivityName.asStateFlow()

        const val ACTION_START = "ACTION_START"; const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"; const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_CANCEL = "ACTION_CANCEL"; const val EXTRA_ACTIVITY_ID = "EXTRA_ACTIVITY_ID"
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
                intent.getIntExtra(EXTRA_ACTIVITY_ID, -1).takeIf { it != -1 }?.let { startTimer(it) }
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_STOP -> stopAndSaveTimer()
            ACTION_CANCEL -> cancelTimer()
        }
        return START_NOT_STICKY
    }

    private fun startTimer(activityId: Int) {
        serviceScope.launch {
            val activity = db.activityDao().getActivityById(activityId)
            activity?.let {
                currentActivity = it
                val (periodStart, periodEnd) = getPeriodTimestamps(it.targetPeriod)
                val logsInPeriod = db.activityDao().getLogsForActivityInRange(it.id, periodStart, periodEnd)
                previouslyLoggedSeconds = logsInPeriod.sumOf { log -> log.durationInSeconds }

                withContext(Dispatchers.Main) {
                    _activeActivityName.value = it.name
                    _activeActivityId.value = activityId
                    _timeElapsed.value = 0
                    _isPaused.value = false
                    startTimeMillis = System.currentTimeMillis()
                    mediaSession.isActive = true

                    timerJob?.cancel()
                    timerJob = serviceScope.launch {
                        while (isActive) {
                            if (!_isPaused.value) {
                                _timeElapsed.update { it + 1 }
                            }
                            // These two must be called every second to update UI
                            updatePlaybackState()
                            updateNotificationMetadata() // Ensures subtitle text updates
                            delay(1000)
                        }
                    }
                    // Initial update and start foreground
                    updatePlaybackState()
                    updateNotificationMetadata()
                    startForeground(NOTIFICATION_ID, createNotification())
                }
            }
        }
    }

    private fun pauseTimer() {
        if (_isPaused.value) return
        _isPaused.value = true
        updatePlaybackState()
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun resumeTimer() {
        if (!_isPaused.value) return
        _isPaused.value = false
        updatePlaybackState()
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun updatePlaybackState() {
        val state = if (_isPaused.value) PlaybackStateCompat.STATE_PAUSED else PlaybackStateCompat.STATE_PLAYING
        val totalProgressMs = ((previouslyLoggedSeconds + _timeElapsed.value) * 1000).toLong()
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_STOP)
            .setState(state, totalProgressMs, 1.0f)
        mediaSession.setPlaybackState(stateBuilder.build())
    }

    // NEW FUNCTION: Separated metadata update logic
    private fun updateNotificationMetadata() {
        val activity = currentActivity ?: return
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, activity.name)
            // *** THE FIX IS HERE ***
            // Set the subtitle to the formatted time of the current session.
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, formatTime(_timeElapsed.value))
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, createColoredBitmap(activity.color))
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, (activity.targetDurationSeconds * 1000).toLong())
            .build()
        mediaSession.setMetadata(metadata)

        // We also need to rebuild the notification itself to see this change immediately
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun stopAndSaveTimer() {
        if (activeActivityId.value == null) return
        val idToSave = activeActivityId.value!!
        val timeToSave = _timeElapsed.value
        if (timeToSave > 0) {
            serviceScope.launch {
                db.activityDao().insertTimeLog(
                    TimeLogEntry(activityId = idToSave, startTime = startTimeMillis, durationInSeconds = timeToSave)
                )
            }
        }
        resetStateAndStopService()
    }

    private fun cancelTimer() {
        resetStateAndStopService()
    }

    private fun resetStateAndStopService() {
        timerJob?.cancel()
        mediaSession.isActive = false
        _activeActivityId.value = null
        _timeElapsed.value = 0
        _isPaused.value = false
        _activeActivityName.value = "Timer"
        currentActivity = null
        previouslyLoggedSeconds = 0
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(): Notification {
        val pCancel = PendingIntent.getService(this, 1, Intent(this, TimerService::class.java).apply { action = ACTION_CANCEL }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val pPauseResume = if (_isPaused.value) {
            PendingIntent.getService(this, 2, Intent(this, TimerService::class.java).apply { action = ACTION_RESUME }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        } else {
            PendingIntent.getService(this, 2, Intent(this, TimerService::class.java).apply { action = ACTION_PAUSE }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val pStop = PendingIntent.getService(this, 3, Intent(this, TimerService::class.java).apply { action = ACTION_STOP }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val pauseResumeIcon = if (_isPaused.value) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause
        val pauseResumeTitle = if (_isPaused.value) "Resume" else "Pause"

        // The builder now doesn't need to set text, as the system pulls it from the metadata.
        val builder = NotificationCompat.Builder(this, "TIMER_SERVICE_CHANNEL")
            .setStyle(
                MediaNotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setLargeIcon(createColoredBitmap(currentActivity?.color))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", pCancel)
            .addAction(pauseResumeIcon, pauseResumeTitle, pPauseResume)
            .addAction(android.R.drawable.ic_media_next, "Stop", pStop)

        return builder.build()
    }

    private fun getPeriodTimestamps(period: String): Pair<Long, Long> {
        val now = Calendar.getInstance()
        val start = now.clone() as Calendar
        start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0)
        when (period) {
            "Weekly" -> { start.firstDayOfWeek = Calendar.MONDAY; start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY) }
            "Monthly" -> { start.set(Calendar.DAY_OF_MONTH, 1) }
        }
        return Pair(start.timeInMillis, now.timeInMillis)
    }

    private fun createColoredBitmap(hexColor: String?): Bitmap {
        val color = try { Color.parseColor(hexColor ?: "#808080") } catch (e: Exception) { Color.GRAY }
        val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(color)
        return bitmap
    }

    private fun formatTime(seconds: Int) = String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60)
    override fun onDestroy() { super.onDestroy(); timerJob?.cancel(); mediaSession.release() }
    override fun onBind(intent: Intent?): IBinder? = null
}