package com.deepak.alarm.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLite database helper for alarm storage.
 * Uses raw SQLite instead of Room to avoid KSP/KAPT dependency issues
 * and keep the native engine zero-dependency.
 */
class AlarmDbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "alarm_database.db"
        const val DATABASE_VERSION = 1

        // Alarm table
        const val TABLE_ALARMS = "alarms"
        const val COL_ID = "id"
        const val COL_LABEL = "label"
        const val COL_HOUR = "hour"
        const val COL_MINUTE = "minute"
        const val COL_ENABLED = "enabled"
        const val COL_REPEAT_DAYS = "repeat_days"
        const val COL_SOUND_URI = "sound_uri"
        const val COL_VIBRATION_ENABLED = "vibration_enabled"
        const val COL_SNOOZE_MINUTES = "snooze_minutes"
        const val COL_CREATED_AT = "created_at"
        const val COL_UPDATED_AT = "updated_at"

        // Alarm instance table
        const val TABLE_INSTANCES = "alarm_instances"
        const val COL_INSTANCE_ID = "instance_id"
        const val COL_ALARM_ID = "alarm_id"
        const val COL_TRIGGER_TIME = "trigger_time"
        const val COL_STATUS = "status"

        @Volatile
        private var instance: AlarmDbHelper? = null

        fun getInstance(context: Context): AlarmDbHelper {
            return instance ?: synchronized(this) {
                instance ?: AlarmDbHelper(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_ALARMS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_LABEL TEXT NOT NULL DEFAULT '',
                $COL_HOUR INTEGER NOT NULL DEFAULT 0,
                $COL_MINUTE INTEGER NOT NULL DEFAULT 0,
                $COL_ENABLED INTEGER NOT NULL DEFAULT 1,
                $COL_REPEAT_DAYS INTEGER NOT NULL DEFAULT 0,
                $COL_SOUND_URI TEXT NOT NULL DEFAULT '',
                $COL_VIBRATION_ENABLED INTEGER NOT NULL DEFAULT 1,
                $COL_SNOOZE_MINUTES INTEGER NOT NULL DEFAULT 10,
                $COL_CREATED_AT INTEGER NOT NULL DEFAULT 0,
                $COL_UPDATED_AT INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_INSTANCES (
                $COL_INSTANCE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ALARM_ID INTEGER NOT NULL,
                $COL_TRIGGER_TIME INTEGER NOT NULL DEFAULT 0,
                $COL_STATUS TEXT NOT NULL DEFAULT 'scheduled',
                FOREIGN KEY ($COL_ALARM_ID) REFERENCES $TABLE_ALARMS($COL_ID) ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX idx_instances_alarm_id ON $TABLE_INSTANCES($COL_ALARM_ID)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX idx_instances_status ON $TABLE_INSTANCES($COL_STATUS)
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future migrations go here
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }
}
