package com.deepak.alarm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import androidx.core.app.NotificationCompat
import com.deepak.alarm.R
import com.deepak.alarm.ui.AlarmActivity

/**
 * Creates notification channels and builds alarm notifications.
 * Uses CATEGORY_ALARM and USAGE_ALARM for DND bypass.
 */
object NotificationHelper {

    const val CHANNEL_ID_ALARM = "alarm_channel"
    const val CHANNEL_ID_FOREGROUND = "alarm_foreground_channel"
    const val NOTIFICATION_ID_ALARM = 1001

    fun createNotificationChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Alarm channel — high importance, DND bypass, alarm audio attributes
        val alarmChannel = NotificationChannel(
            CHANNEL_ID_ALARM,
            "Alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alarm notifications"
            setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(
                null, // Sound handled by MediaPlayer
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            enableVibration(false) // Vibration handled separately
        }
        nm.createNotificationChannel(alarmChannel)

        // Foreground service channel — low importance, silent
        val foregroundChannel = NotificationChannel(
            CHANNEL_ID_FOREGROUND,
            "Alarm Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ongoing alarm service notification"
            setShowBadge(false)
        }
        nm.createNotificationChannel(foregroundChannel)
    }

    /**
     * Build the full-screen alarm notification with snooze/dismiss actions.
     */
    fun buildAlarmNotification(
        context: Context,
        alarmId: Long,
        instanceId: Long,
        label: String
    ): Notification {
        // Full-screen intent → AlarmActivity
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra(AlarmActivity.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmActivity.EXTRA_INSTANCE_ID, instanceId)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action
        val snoozeIntent = Intent(context, AlarmForegroundService::class.java).apply {
            action = AlarmForegroundService.ACTION_SNOOZE
            putExtra(AlarmForegroundService.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmForegroundService.EXTRA_INSTANCE_ID, instanceId)
        }
        val snoozePendingIntent = PendingIntent.getService(
            context,
            alarmId.toInt() + 10000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action
        val dismissIntent = Intent(context, AlarmForegroundService::class.java).apply {
            action = AlarmForegroundService.ACTION_DISMISS
            putExtra(AlarmForegroundService.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmForegroundService.EXTRA_INSTANCE_ID, instanceId)
        }
        val dismissPendingIntent = PendingIntent.getService(
            context,
            alarmId.toInt() + 20000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayLabel = if (label.isNotEmpty()) label else "Alarm"

        return NotificationCompat.Builder(context, CHANNEL_ID_ALARM)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(displayLabel)
            .setContentText("Alarm is ringing")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(R.drawable.ic_snooze, "Snooze", snoozePendingIntent)
            .addAction(R.drawable.ic_dismiss, "Dismiss", dismissPendingIntent)
            .build()
    }

    /**
     * Show a simple push notification indicating that an alarm was missed.
     */
    fun showMissedAlarmNotification(context: Context, alarmId: Long, label: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val displayLabel = if (label.isNotEmpty()) label else "Alarm"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALARM)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Missed Alarm")
            .setContentText(displayLabel)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
            
        nm.notify((alarmId + 50000).toInt(), notification)
    }
}
