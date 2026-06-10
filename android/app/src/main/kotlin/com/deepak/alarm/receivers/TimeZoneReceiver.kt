package com.deepak.alarm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.deepak.alarm.engine.AlarmScheduler

/**
 * Recalculates and reschedules all alarms when timezone changes.
 */
class TimeZoneReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TimeZoneReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            Log.d(TAG, "Timezone changed, rescheduling all alarms")
            Thread {
                AlarmScheduler.rescheduleAllAlarms(context)
            }.start()
        }
    }
}
