package com.deepak.alarm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.deepak.alarm.engine.AlarmScheduler

/**
 * Reschedules all alarms after the app is updated (MY_PACKAGE_REPLACED).
 */
class PackageUpdateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PackageUpdateReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d(TAG, "Package replaced, rescheduling all alarms")
            Thread {
                AlarmScheduler.rescheduleAllAlarms(context)
            }.start()
        }
    }
}
