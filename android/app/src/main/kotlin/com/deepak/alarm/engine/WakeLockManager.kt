package com.deepak.alarm.engine

import android.content.Context
import android.os.PowerManager
import android.util.Log

/**
 * Manages partial wake locks for alarm delivery reliability.
 * Acquires for max 10 seconds; released as soon as foreground service starts.
 */
object WakeLockManager {

    private const val TAG = "WakeLockManager"
    private const val WAKE_LOCK_TAG = "com.deepak.alarm:AlarmWakeLock"
    private const val DEFAULT_TIMEOUT_MS = 10_000L

    private var wakeLock: PowerManager.WakeLock? = null

    fun acquire(context: Context, timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        if (wakeLock?.isHeld == true) {
            Log.w(TAG, "Wake lock already held, releasing first")
            release()
        }

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            acquire(timeoutMs)
        }
        Log.d(TAG, "Wake lock acquired for ${timeoutMs}ms")
    }

    fun release() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "Wake lock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock", e)
        } finally {
            wakeLock = null
        }
    }
}
