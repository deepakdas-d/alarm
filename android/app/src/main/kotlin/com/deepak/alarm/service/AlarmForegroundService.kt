package com.deepak.alarm.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.deepak.alarm.data.AlarmEntity
import com.deepak.alarm.data.AlarmInstanceEntity
import com.deepak.alarm.data.AlarmRepository
import com.deepak.alarm.engine.AlarmScheduler
import com.deepak.alarm.engine.WakeLockManager
import com.deepak.alarm.ui.AlarmActivity

/**
 * Foreground service that manages alarm playback, vibration, and notification.
 * This is the core alarm execution engine — operates entirely without Flutter.
 */
class AlarmForegroundService : Service() {

    companion object {
        private const val TAG = "AlarmForegroundService"
        const val ACTION_START_ALARM = "com.deepak.alarm.ACTION_START_ALARM"
        const val ACTION_SNOOZE = "com.deepak.alarm.ACTION_SNOOZE"
        const val ACTION_DISMISS = "com.deepak.alarm.ACTION_DISMISS"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_INSTANCE_ID = "extra_instance_id"
        private const val AUTO_DISMISS_MINUTES = 5L
    }

    private var mediaPlayerManager: MediaPlayerManager? = null
    private var vibrator: Vibrator? = null
    private var autoDismissHandler: Handler? = null
    private var currentAlarmId: Long = -1
    private var currentInstanceId: Long = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        mediaPlayerManager = MediaPlayerManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_ALARM -> {
                val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
                val instanceId = intent.getLongExtra(EXTRA_INSTANCE_ID, -1)
                if (alarmId != -1L) {
                    startAlarm(alarmId, instanceId)
                }
            }
            ACTION_SNOOZE -> {
                val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, currentAlarmId)
                val instanceId = intent.getLongExtra(EXTRA_INSTANCE_ID, currentInstanceId)
                snoozeAlarm(alarmId, instanceId)
            }
            ACTION_DISMISS -> {
                val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, currentAlarmId)
                val instanceId = intent.getLongExtra(EXTRA_INSTANCE_ID, currentInstanceId)
                dismissAlarm(alarmId, instanceId)
            }
        }
        return START_NOT_STICKY
    }

    private fun startAlarm(alarmId: Long, instanceId: Long) {
        Log.d(TAG, "Starting alarm: alarmId=$alarmId, instanceId=$instanceId")

        currentAlarmId = alarmId
        currentInstanceId = instanceId

        val repo = AlarmRepository(this)
        val alarm = repo.getAlarm(alarmId)

        if (alarm == null) {
            Log.e(TAG, "Alarm not found in database: $alarmId")
            WakeLockManager.release()
            stopSelf()
            return
        }

        // Update instance status
        if (instanceId > 0) {
            repo.updateInstanceStatus(instanceId, AlarmInstanceEntity.STATUS_TRIGGERED)
        }

        // Start foreground with notification
        val notification = NotificationHelper.buildAlarmNotification(
            this, alarmId, instanceId, alarm.label
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationHelper.NOTIFICATION_ID_ALARM,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID_ALARM, notification)
        }

        // Release the wake lock from AlarmReceiver (service is now foreground)
        WakeLockManager.release()

        // Start audio
        mediaPlayerManager?.start(alarm.soundUri, increaseVolume = true)

        // Start vibration
        if (alarm.vibrationEnabled) {
            startVibration()
        }

        // Launch full-screen AlarmActivity
        launchAlarmActivity(alarmId, instanceId)

        // Auto-dismiss after timeout
        scheduleAutoDismiss(alarmId, instanceId)
    }

    private fun snoozeAlarm(alarmId: Long, instanceId: Long) {
        Log.d(TAG, "Snoozing alarm: alarmId=$alarmId")

        stopAlarmOutput()

        val repo = AlarmRepository(this)
        val alarm = repo.getAlarm(alarmId)

        if (alarm != null) {
            // Update current instance
            if (instanceId > 0) {
                repo.updateInstanceStatus(instanceId, AlarmInstanceEntity.STATUS_SNOOZED)
            }

            // Schedule snooze alarm
            AlarmScheduler.scheduleSnooze(this, alarm, alarm.snoozeMinutes)
        }

        stopSelf()
    }

    private fun dismissAlarm(alarmId: Long, instanceId: Long) {
        Log.d(TAG, "Dismissing alarm: alarmId=$alarmId")

        stopAlarmOutput()

        val repo = AlarmRepository(this)
        val alarm = repo.getAlarm(alarmId)

        if (alarm != null) {
            // Update instance status
            if (instanceId > 0) {
                repo.updateInstanceStatus(instanceId, AlarmInstanceEntity.STATUS_DISMISSED)
            }

            // Schedule next occurrence for repeating alarms
            if (alarm.isRepeating()) {
                AlarmScheduler.scheduleNextAlarm(this, alarm)
            } else {
                // Disable one-shot alarm
                repo.setAlarmEnabled(alarmId, false)
            }
        }

        stopSelf()
    }

    private fun stopAlarmOutput() {
        autoDismissHandler?.removeCallbacksAndMessages(null)
        autoDismissHandler = null
        mediaPlayerManager?.stop()
        stopVibration()

        // Cancel notification
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.cancel(NotificationHelper.NOTIFICATION_ID_ALARM)
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // 500ms ON, 500ms OFF, repeat
        val pattern = longArrayOf(0, 500, 500)
        vibrator?.vibrate(
            VibrationEffect.createWaveform(pattern, 0) // 0 = repeat from index 0
        )
    }

    private fun stopVibration() {
        vibrator?.cancel()
        vibrator = null
    }

    private fun launchAlarmActivity(alarmId: Long, instanceId: Long) {
        val intent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra(AlarmActivity.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmActivity.EXTRA_INSTANCE_ID, instanceId)
        }
        startActivity(intent)
    }

    private fun scheduleAutoDismiss(alarmId: Long, instanceId: Long) {
        autoDismissHandler = Handler(Looper.getMainLooper())
        autoDismissHandler?.postDelayed({
            Log.d(TAG, "Auto-dismissing alarm $alarmId after $AUTO_DISMISS_MINUTES minutes")
            dismissAlarm(alarmId, instanceId)
        }, AUTO_DISMISS_MINUTES * 60 * 1000)
    }

    override fun onDestroy() {
        stopAlarmOutput()
        mediaPlayerManager = null
        super.onDestroy()
    }
}
