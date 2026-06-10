package com.deepak.alarm.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor

/**
 * Repository providing CRUD operations for alarms and alarm instances.
 * All operations are synchronous – call from background thread if needed.
 */
class AlarmRepository(context: Context) {

    private val dbHelper = AlarmDbHelper.getInstance(context)

    // ─── Alarm CRUD ──────────────────────────────────────────────

    fun insertAlarm(alarm: AlarmEntity): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(AlarmDbHelper.COL_LABEL, alarm.label)
            put(AlarmDbHelper.COL_HOUR, alarm.hour)
            put(AlarmDbHelper.COL_MINUTE, alarm.minute)
            put(AlarmDbHelper.COL_ENABLED, if (alarm.enabled) 1 else 0)
            put(AlarmDbHelper.COL_REPEAT_DAYS, alarm.repeatDays)
            put(AlarmDbHelper.COL_SOUND_URI, alarm.soundUri)
            put(AlarmDbHelper.COL_VIBRATION_ENABLED, if (alarm.vibrationEnabled) 1 else 0)
            put(AlarmDbHelper.COL_SNOOZE_MINUTES, alarm.snoozeMinutes)
            put(AlarmDbHelper.COL_CREATED_AT, System.currentTimeMillis())
            put(AlarmDbHelper.COL_UPDATED_AT, System.currentTimeMillis())
        }
        return db.insert(AlarmDbHelper.TABLE_ALARMS, null, values)
    }

    fun updateAlarm(alarm: AlarmEntity): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(AlarmDbHelper.COL_LABEL, alarm.label)
            put(AlarmDbHelper.COL_HOUR, alarm.hour)
            put(AlarmDbHelper.COL_MINUTE, alarm.minute)
            put(AlarmDbHelper.COL_ENABLED, if (alarm.enabled) 1 else 0)
            put(AlarmDbHelper.COL_REPEAT_DAYS, alarm.repeatDays)
            put(AlarmDbHelper.COL_SOUND_URI, alarm.soundUri)
            put(AlarmDbHelper.COL_VIBRATION_ENABLED, if (alarm.vibrationEnabled) 1 else 0)
            put(AlarmDbHelper.COL_SNOOZE_MINUTES, alarm.snoozeMinutes)
            put(AlarmDbHelper.COL_UPDATED_AT, System.currentTimeMillis())
        }
        return db.update(
            AlarmDbHelper.TABLE_ALARMS, values,
            "${AlarmDbHelper.COL_ID} = ?", arrayOf(alarm.id.toString())
        )
    }

    fun deleteAlarm(alarmId: Long): Int {
        val db = dbHelper.writableDatabase
        // Instances deleted via CASCADE
        return db.delete(
            AlarmDbHelper.TABLE_ALARMS,
            "${AlarmDbHelper.COL_ID} = ?", arrayOf(alarmId.toString())
        )
    }

    fun getAlarm(alarmId: Long): AlarmEntity? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            AlarmDbHelper.TABLE_ALARMS, null,
            "${AlarmDbHelper.COL_ID} = ?", arrayOf(alarmId.toString()),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) cursorToAlarm(it) else null
        }
    }

    fun getAllAlarms(): List<AlarmEntity> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            AlarmDbHelper.TABLE_ALARMS, null,
            null, null, null, null,
            "${AlarmDbHelper.COL_HOUR} ASC, ${AlarmDbHelper.COL_MINUTE} ASC"
        )
        return cursor.use {
            val list = mutableListOf<AlarmEntity>()
            while (it.moveToNext()) {
                list.add(cursorToAlarm(it))
            }
            list
        }
    }

    fun getEnabledAlarms(): List<AlarmEntity> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            AlarmDbHelper.TABLE_ALARMS, null,
            "${AlarmDbHelper.COL_ENABLED} = 1", null,
            null, null, null
        )
        return cursor.use {
            val list = mutableListOf<AlarmEntity>()
            while (it.moveToNext()) {
                list.add(cursorToAlarm(it))
            }
            list
        }
    }

    fun setAlarmEnabled(alarmId: Long, enabled: Boolean): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(AlarmDbHelper.COL_ENABLED, if (enabled) 1 else 0)
            put(AlarmDbHelper.COL_UPDATED_AT, System.currentTimeMillis())
        }
        return db.update(
            AlarmDbHelper.TABLE_ALARMS, values,
            "${AlarmDbHelper.COL_ID} = ?", arrayOf(alarmId.toString())
        )
    }

    // ─── Alarm Instance CRUD ─────────────────────────────────────

    fun insertInstance(instance: AlarmInstanceEntity): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(AlarmDbHelper.COL_ALARM_ID, instance.alarmId)
            put(AlarmDbHelper.COL_TRIGGER_TIME, instance.triggerTime)
            put(AlarmDbHelper.COL_STATUS, instance.status)
        }
        return db.insert(AlarmDbHelper.TABLE_INSTANCES, null, values)
    }

    fun updateInstanceStatus(instanceId: Long, status: String): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(AlarmDbHelper.COL_STATUS, status)
        }
        return db.update(
            AlarmDbHelper.TABLE_INSTANCES, values,
            "${AlarmDbHelper.COL_INSTANCE_ID} = ?", arrayOf(instanceId.toString())
        )
    }

    fun getInstance(instanceId: Long): AlarmInstanceEntity? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            AlarmDbHelper.TABLE_INSTANCES, null,
            "${AlarmDbHelper.COL_INSTANCE_ID} = ?", arrayOf(instanceId.toString()),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) cursorToInstance(it) else null
        }
    }

    fun getInstancesForAlarm(alarmId: Long): List<AlarmInstanceEntity> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            AlarmDbHelper.TABLE_INSTANCES, null,
            "${AlarmDbHelper.COL_ALARM_ID} = ?", arrayOf(alarmId.toString()),
            null, null, "${AlarmDbHelper.COL_TRIGGER_TIME} DESC"
        )
        return cursor.use {
            val list = mutableListOf<AlarmInstanceEntity>()
            while (it.moveToNext()) {
                list.add(cursorToInstance(it))
            }
            list
        }
    }

    fun getScheduledInstances(): List<AlarmInstanceEntity> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            AlarmDbHelper.TABLE_INSTANCES, null,
            "${AlarmDbHelper.COL_STATUS} = ?",
            arrayOf(AlarmInstanceEntity.STATUS_SCHEDULED),
            null, null, "${AlarmDbHelper.COL_TRIGGER_TIME} ASC"
        )
        return cursor.use {
            val list = mutableListOf<AlarmInstanceEntity>()
            while (it.moveToNext()) {
                list.add(cursorToInstance(it))
            }
            list
        }
    }

    fun getStatistics(): Map<String, Int> {
        val stats = mutableMapOf<String, Int>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT ${AlarmDbHelper.COL_STATUS}, COUNT(*) FROM ${AlarmDbHelper.TABLE_INSTANCES} GROUP BY ${AlarmDbHelper.COL_STATUS}",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                val status = it.getString(0) ?: continue
                val count = it.getInt(1)
                stats[status] = count
            }
        }
        return stats
    }

    fun deleteInstance(instanceId: Long): Int {
        val db = dbHelper.writableDatabase
        return db.delete(
            AlarmDbHelper.TABLE_INSTANCES,
            "${AlarmDbHelper.COL_INSTANCE_ID} = ?", arrayOf(instanceId.toString())
        )
    }

    fun deleteInstancesForAlarm(alarmId: Long): Int {
        val db = dbHelper.writableDatabase
        return db.delete(
            AlarmDbHelper.TABLE_INSTANCES,
            "${AlarmDbHelper.COL_ALARM_ID} = ?", arrayOf(alarmId.toString())
        )
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private fun cursorToAlarm(cursor: Cursor): AlarmEntity {
        return AlarmEntity(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(AlarmDbHelper.COL_ID)),
            label = cursor.getString(cursor.getColumnIndexOrThrow(AlarmDbHelper.COL_LABEL)),
            hour = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmDbHelper.COL_HOUR)),
            minute = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmDbHelper.COL_MINUTE)),
            enabled = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmDbHelper.COL_ENABLED)) == 1,
            repeatDays = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmDbHelper.COL_REPEAT_DAYS)),
            soundUri = cursor.getString(cursor.getColumnIndexOrThrow(AlarmDbHelper.COL_SOUND_URI)),
            vibrationEnabled = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmDbHelper.COL_VIBRATION_ENABLED)) == 1,
            snoozeMinutes = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmDbHelper.COL_SNOOZE_MINUTES)),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(AlarmDbHelper.COL_CREATED_AT)),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(AlarmDbHelper.COL_UPDATED_AT))
        )
    }

    private fun cursorToInstance(cursor: Cursor): AlarmInstanceEntity {
        return AlarmInstanceEntity(
            instanceId = cursor.getLong(cursor.getColumnIndexOrThrow(AlarmDbHelper.COL_INSTANCE_ID)),
            alarmId = cursor.getLong(cursor.getColumnIndexOrThrow(AlarmDbHelper.COL_ALARM_ID)),
            triggerTime = cursor.getLong(cursor.getColumnIndexOrThrow(AlarmDbHelper.COL_TRIGGER_TIME)),
            status = cursor.getString(cursor.getColumnIndexOrThrow(AlarmDbHelper.COL_STATUS))
        )
    }
}
