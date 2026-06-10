package com.deepak.alarm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.deepak.alarm.engine.WakeLockManager
import com.deepak.alarm.service.AlarmForegroundService

/**
 * Receives alarm broadcasts from AlarmManager.
 * Acquires a wake lock and starts the foreground service.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "AlarmReceiver"
        const val ACTION_ALARM_TRIGGER = "com.deepak.alarm.ACTION_ALARM_TRIGGER"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_INSTANCE_ID = "extra_instance_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ALARM_TRIGGER) return

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
        val instanceId = intent.getLongExtra(EXTRA_INSTANCE_ID, -1)

        Log.d(TAG, "Alarm triggered: alarmId=$alarmId, instanceId=$instanceId")

        if (alarmId == -1L) {
            Log.e(TAG, "Invalid alarm ID")
            return
        }

        // Acquire wake lock immediately (max 10 seconds)
        WakeLockManager.acquire(context)

        // Start foreground service to play alarm
        val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
            action = AlarmForegroundService.ACTION_START_ALARM
            putExtra(AlarmForegroundService.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmForegroundService.EXTRA_INSTANCE_ID, instanceId)
        }

        context.startForegroundService(serviceIntent)
    }
}
