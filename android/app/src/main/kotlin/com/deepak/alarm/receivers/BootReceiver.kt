package com.deepak.alarm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.deepak.alarm.engine.AlarmScheduler

/**
 * Reschedules all alarms after device reboot.
 * Handles both BOOT_COMPLETED and LOCKED_BOOT_COMPLETED.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, rescheduling all alarms")
            Thread {
                AlarmScheduler.checkAndNotifyMissedAlarms(context)
                AlarmScheduler.rescheduleAllAlarms(context)
            }.start()
        }
    }
}
