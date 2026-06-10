package com.deepak.alarm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.deepak.alarm.engine.AlarmScheduler

/**
 * Reschedules all alarms when system time is manually changed (TIME_SET).
 */
class TimeChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TimeChangeReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_TIME_CHANGED) {
            Log.d(TAG, "System time changed, rescheduling all alarms")
            Thread {
                AlarmScheduler.rescheduleAllAlarms(context)
            }.start()
        }
    }
}
