package com.deepak.alarm.ui

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Button
import android.widget.TextClock
import android.widget.TextView
import com.deepak.alarm.R
import com.deepak.alarm.data.AlarmRepository
import com.deepak.alarm.service.AlarmForegroundService

/**
 * Full-screen alarm activity displayed over the lock screen.
 * This is a native Android Activity — does NOT depend on Flutter engine.
 */
class AlarmActivity : Activity() {

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_INSTANCE_ID = "extra_instance_id"
    }

    private var alarmId: Long = -1
    private var instanceId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen and turn screen on
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        // Keep screen on
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        setContentView(R.layout.activity_alarm)

        // Make fullscreen / immersive
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }

        alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
        instanceId = intent.getLongExtra(EXTRA_INSTANCE_ID, -1)

        setupUI()
    }

    private fun setupUI() {
        // Load alarm label
        val labelView = findViewById<TextView>(R.id.alarmLabel)
        if (alarmId > 0) {
            val repo = AlarmRepository(this)
            val alarm = repo.getAlarm(alarmId)
            labelView.text = if (alarm?.label?.isNotEmpty() == true) alarm.label else "Alarm"
        } else {
            labelView.text = "Alarm"
        }

        // Snooze button
        val snoozeButton = findViewById<Button>(R.id.btnSnooze)
        snoozeButton.setOnClickListener {
            sendActionToService(AlarmForegroundService.ACTION_SNOOZE)
            finishAndRemoveTask()
        }

        // Dismiss button
        val dismissButton = findViewById<Button>(R.id.btnDismiss)
        dismissButton.setOnClickListener {
            sendActionToService(AlarmForegroundService.ACTION_DISMISS)
            finishAndRemoveTask()
        }
    }

    private fun sendActionToService(action: String) {
        val intent = Intent(this, AlarmForegroundService::class.java).apply {
            this.action = action
            putExtra(AlarmForegroundService.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmForegroundService.EXTRA_INSTANCE_ID, instanceId)
        }
        startService(intent)
    }

    @Deprecated("Use onBackPressedDispatcher instead")
    override fun onBackPressed() {
        // Pressing back snoozes the alarm
        sendActionToService(AlarmForegroundService.ACTION_SNOOZE)
        finishAndRemoveTask()
    }
}
