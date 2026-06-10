package com.deepak.alarm.engine

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.deepak.alarm.data.AlarmEntity
import com.deepak.alarm.data.AlarmInstanceEntity
import com.deepak.alarm.data.AlarmRepository
import com.deepak.alarm.receivers.AlarmReceiver
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/**
 * Schedules and cancels alarms using AlarmManager.setExactAndAllowWhileIdle().
 * Each alarm gets a unique PendingIntent via its alarmId.
 */
object AlarmScheduler {

    private const val TAG = "AlarmScheduler"

    /**
     * Schedule an alarm at the given trigger time (epoch millis).
     */
    fun scheduleAlarm(context: Context, alarmId: Long, instanceId: Long, triggerTimeMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.e(TAG, "Cannot schedule exact alarm — permission not granted")
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_INSTANCE_ID, instanceId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTimeMillis,
            pendingIntent
        )

        Log.d(TAG, "Scheduled alarm id=$alarmId instance=$instanceId at $triggerTimeMillis")
    }

    /**
     * Cancel a scheduled alarm.
     */
    fun cancelAlarm(context: Context, alarmId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Cancelled alarm id=$alarmId")
    }

    /**
     * Reschedule all enabled alarms. Called on boot, time change, timezone change, app update.
     */
    fun rescheduleAllAlarms(context: Context) {
        val repo = AlarmRepository(context)
        val enabledAlarms = repo.getEnabledAlarms()

        Log.d(TAG, "Rescheduling ${enabledAlarms.size} enabled alarms")

        for (alarm in enabledAlarms) {
            scheduleNextAlarm(context, alarm)
        }
    }

    /**
     * Calculate the next trigger time and schedule it. Creates a new instance in the DB.
     */
    fun scheduleNextAlarm(context: Context, alarm: AlarmEntity) {
        val repo = AlarmRepository(context)
        val triggerTime = calculateNextTriggerTime(alarm.hour, alarm.minute, alarm.repeatDays)

        if (triggerTime <= 0) {
            Log.w(TAG, "Could not calculate trigger time for alarm ${alarm.id}")
            return
        }

        // Delete existing scheduled instances for this alarm
        val existingInstances = repo.getInstancesForAlarm(alarm.id)
        for (inst in existingInstances) {
            if (inst.status == AlarmInstanceEntity.STATUS_SCHEDULED) {
                repo.deleteInstance(inst.instanceId)
            }
        }

        // Create new instance
        val instance = AlarmInstanceEntity(
            alarmId = alarm.id,
            triggerTime = triggerTime,
            status = AlarmInstanceEntity.STATUS_SCHEDULED
        )
        val instanceId = repo.insertInstance(instance)

        // Schedule via AlarmManager
        scheduleAlarm(context, alarm.id, instanceId, triggerTime)
    }

    /**
     * Calculate the next trigger time in epoch millis using timezone-aware APIs.
     *
     * @param hour alarm hour (0-23)
     * @param minute alarm minute (0-59)
     * @param repeatDays bitmask of repeat days (0 = one-shot)
     * @return epoch millis of next trigger, or -1 if error
     */
    fun calculateNextTriggerTime(hour: Int, minute: Int, repeatDays: Int): Long {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val alarmTime = LocalTime.of(hour, minute)

        if (repeatDays == 0) {
            // One-shot alarm: today if time hasn't passed, otherwise tomorrow
            var trigger = ZonedDateTime.of(LocalDate.now(zone), alarmTime, zone)
            if (trigger.isBefore(now) || trigger.isEqual(now)) {
                trigger = trigger.plusDays(1)
            }
            return trigger.toInstant().toEpochMilli()
        }

        // Repeating alarm: find the next matching day
        val dayMapping = mapOf(
            AlarmEntity.MONDAY    to DayOfWeek.MONDAY,
            AlarmEntity.TUESDAY   to DayOfWeek.TUESDAY,
            AlarmEntity.WEDNESDAY to DayOfWeek.WEDNESDAY,
            AlarmEntity.THURSDAY  to DayOfWeek.THURSDAY,
            AlarmEntity.FRIDAY    to DayOfWeek.FRIDAY,
            AlarmEntity.SATURDAY  to DayOfWeek.SATURDAY,
            AlarmEntity.SUNDAY    to DayOfWeek.SUNDAY
        )

        var nearestTrigger: ZonedDateTime? = null

        for ((bit, dayOfWeek) in dayMapping) {
            if ((repeatDays and bit) == 0) continue

            var candidate = ZonedDateTime.of(LocalDate.now(zone), alarmTime, zone)

            if (candidate.dayOfWeek == dayOfWeek) {
                // Same day: use today if time hasn't passed, otherwise next week
                if (candidate.isBefore(now) || candidate.isEqual(now)) {
                    candidate = candidate.plusWeeks(1)
                }
            } else {
                // Different day: find next occurrence
                candidate = candidate.with(TemporalAdjusters.next(dayOfWeek))
            }

            if (nearestTrigger == null || candidate.isBefore(nearestTrigger)) {
                nearestTrigger = candidate
            }
        }

        return nearestTrigger?.toInstant()?.toEpochMilli() ?: -1
    }

    /**
     * Schedule a snooze alarm for the given alarm.
     */
    fun scheduleSnooze(context: Context, alarm: AlarmEntity, snoozeDurationMinutes: Int): Long {
        val repo = AlarmRepository(context)
        val triggerTime = System.currentTimeMillis() + (snoozeDurationMinutes * 60 * 1000L)

        val instance = AlarmInstanceEntity(
            alarmId = alarm.id,
            triggerTime = triggerTime,
            status = AlarmInstanceEntity.STATUS_SNOOZED
        )
        val instanceId = repo.insertInstance(instance)

        scheduleAlarm(context, alarm.id, instanceId, triggerTime)

        Log.d(TAG, "Snoozed alarm ${alarm.id} for $snoozeDurationMinutes minutes, instance=$instanceId")
        return instanceId
    }

    /**
     * Check if there are any scheduled alarm instances whose trigger time was in the past.
     * Mark them as missed and notify the user.
     */
    fun checkAndNotifyMissedAlarms(context: Context) {
        val repo = AlarmRepository(context)
        val enabledAlarms = repo.getEnabledAlarms()
        val now = System.currentTimeMillis()
        // Provide a small grace period e.g., 5 seconds. If missed by more than 5 mins, it's definitely missed.
        // Actually any alarm older than (now - 1 min) is considered missed or delayed.
        for (alarm in enabledAlarms) {
            val instances = repo.getInstancesForAlarm(alarm.id)
            for (inst in instances) {
                if (inst.status == AlarmInstanceEntity.STATUS_SCHEDULED && inst.triggerTime < now - 60000L) {
                    Log.d(TAG, "Detected missed alarm ${alarm.id} instance ${inst.instanceId}")
                    repo.updateInstanceStatus(inst.instanceId, AlarmInstanceEntity.STATUS_MISSED)
                    com.deepak.alarm.service.NotificationHelper.showMissedAlarmNotification(context, alarm.id, alarm.label)
                }
            }
        }
    }
}
