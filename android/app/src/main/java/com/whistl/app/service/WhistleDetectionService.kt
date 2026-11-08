package com.whistl.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.whistl.app.audio.AudioRecorderSource
import com.whistl.app.audio.RingBuffer
import com.whistl.app.ml.WhistleDetector
import com.whistl.app.state.WhistleStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WhistleDetectionService : Service() {
    companion object {
        const val ACTION_START = "com.whistl.app.action.START"
        const val ACTION_STOP = "com.whistl.app.action.STOP"
        const val ACTION_STOP_ALARM = "com.whistl.app.action.STOP_ALARM"
        const val ACTION_ADD_WHISTLE = "com.whistl.app.action.ADD_WHISTLE"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "whistl_listening_channel_v2"
        const val ALARM_CHANNEL_ID = "whistl_alarm_channel"
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var audioRecorder: AudioRecorderSource? = null
    private var whistleDetector: WhistleDetector? = null
    private var ringBuffer: RingBuffer = RingBuffer(15600)
    private var stateMachine: WhistleStateMachine = WhistleStateMachine()
    private lateinit var alarmController: AlarmController

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var inferenceJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        alarmController = AlarmController(this)
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startListening()
            ACTION_STOP -> stopListening()
            ACTION_STOP_ALARM -> handleStopAlarm()
            ACTION_ADD_WHISTLE -> handleAddWhistle()
        }
        return START_STICKY
    }

    private fun startListening() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Whistl::ListeningLock").apply {
                acquire(60 * 60 * 1000L) // 1 hour safety limit
            }
        }

        val notification = buildNotification(AppSessionRepository.sessionState.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        whistleDetector = WhistleDetector(this)
        ringBuffer.clear()

        // Preserve current count if user is adding more whistles to an ongoing session
        val existingCount = AppSessionRepository.sessionState.value.currentCount
        stateMachine.reset(initialCount = existingCount)

        AppSessionRepository.updateState { it.copy(isListening = true, isTargetReached = false) }

        audioRecorder?.stop()
        audioRecorder = AudioRecorderSource(sampleRate = 16000)
        audioRecorder?.start { samples, length ->
            ringBuffer.write(samples, length)
        }

        inferenceJob?.cancel()
        inferenceJob = serviceScope.launch {
            val inputBuffer = FloatArray(15600)
            var lastNotifiedCount = -1
            var lastTargetReached = false
            var lastTarget = -1

            while (isActive) {
                ringBuffer.readNormalizedFloatSnapshot(inputBuffer)
                val result = whistleDetector?.detect(inputBuffer)
                val conf = result?.whistleProb ?: 0f
                val now = System.currentTimeMillis()

                val event = stateMachine.process(conf, now)
                val count = stateMachine.whistleCount
                val target = AppSessionRepository.sessionState.value.targetCount
                val targetReached = count >= target

                AppSessionRepository.updateState { current ->
                    val newHistory = if (event != null) current.whistleHistory + event else current.whistleHistory
                    current.copy(
                        currentCount = count,
                        whistleState = stateMachine.currentState,
                        currentConfidence = conf,
                        isTargetReached = targetReached,
                        isListening = !targetReached,
                        whistleHistory = newHistory
                    )
                }

                // ONLY update notification when count, target, or targetReached actually changes
                if (count != lastNotifiedCount || targetReached != lastTargetReached || target != lastTarget) {
                    lastNotifiedCount = count
                    lastTargetReached = targetReached
                    lastTarget = target
                    updateNotification()
                }

                if (targetReached) {
                    if (!alarmController.isAlarmPlaying) {
                        alarmController.triggerAlarm()
                    }
                    try {
                        audioRecorder?.stop()
                    } catch (_: Exception) {}
                    audioRecorder = null
                    try {
                        whistleDetector?.close()
                    } catch (_: Exception) {}
                    whistleDetector = null
                    break
                }

                delay(400)
            }
        }
    }

    private fun handleAddWhistle() {
        alarmController.stopAlarm()
        val current = AppSessionRepository.sessionState.value
        val newTarget = maxOf(current.currentCount + 1, current.targetCount + 1)
        AppSessionRepository.updateState {
            it.copy(
                targetCount = newTarget,
                isTargetReached = false,
                isListening = true
            )
        }
        startListening()
    }

    private fun handleStopAlarm() {
        alarmController.stopAlarm()
        AppSessionRepository.updateState { it.copy(isTargetReached = false, isListening = false) }
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopListening() {
        inferenceJob?.cancel()
        inferenceJob = null
        audioRecorder?.stop()
        audioRecorder = null
        whistleDetector?.close()
        whistleDetector = null
        alarmController.stopAlarm()

        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null

        AppSessionRepository.updateState { it.copy(isListening = false) }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Default importance listening service channel (alerting by default, not silent)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Whistl Listening Service",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Displays active listening status for cooker whistles"
            }
            manager?.createNotificationChannel(channel)

            // High importance alarm channel for when target is reached
            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "Whistle Target Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority alarm when whistle target is reached"
                enableVibration(true)
            }
            manager?.createNotificationChannel(alarmChannel)
        }
    }

    private fun buildNotification(state: AppSessionState): Notification {
        val channelId = if (state.isTargetReached) ALARM_CHANNEL_ID else CHANNEL_ID

        val contentText = if (state.isTargetReached) {
            "TARGET REACHED! ${state.currentCount}/${state.targetCount} whistles completed"
        } else {
            "Whistles: ${state.currentCount} / ${state.targetCount} • Listening..."
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Whistl — Cooker Counter")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (state.isTargetReached) {
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
            builder.setPriority(NotificationCompat.PRIORITY_HIGH)

            // Action 1: Stop Alarm
            val stopAlarmIntent = Intent(this, WhistleDetectionService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            val stopAlarmPending = PendingIntent.getService(
                this,
                101,
                stopAlarmIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Alarm", stopAlarmPending)

            // Action 2: +1 Whistle (Keep Listening)
            val addWhistleIntent = Intent(this, WhistleDetectionService::class.java).apply {
                action = ACTION_ADD_WHISTLE
            }
            val addWhistlePending = PendingIntent.getService(
                this,
                102,
                addWhistleIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(android.R.drawable.ic_input_add, "+1 Whistle", addWhistlePending)
        } else {
            // Stop Listening Action
            val stopIntent = Intent(this, WhistleDetectionService::class.java).apply {
                action = ACTION_STOP
            }
            val stopPending = PendingIntent.getService(
                this,
                100,
                stopIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)
        }

        return builder.build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(AppSessionRepository.sessionState.value))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopListening()
        super.onDestroy()
    }
}
