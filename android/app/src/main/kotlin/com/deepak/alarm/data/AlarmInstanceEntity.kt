package com.deepak.alarm.data

/**
 * Represents a specific occurrence of an alarm (scheduled, snoozed, etc.).
 */
data class AlarmInstanceEntity(
    val instanceId: Long = 0,
    val alarmId: Long = 0,
    val triggerTime: Long = 0,
    val status: String = STATUS_SCHEDULED
) {
    companion object {
        const val STATUS_SCHEDULED  = "scheduled"
        const val STATUS_TRIGGERED  = "triggered"
        const val STATUS_DISMISSED  = "dismissed"
        const val STATUS_SNOOZED    = "snoozed"
        const val STATUS_MISSED     = "missed"

        fun fromMap(map: Map<String, Any?>): AlarmInstanceEntity {
            return AlarmInstanceEntity(
                instanceId = (map["instanceId"] as? Number)?.toLong() ?: 0,
                alarmId = (map["alarmId"] as? Number)?.toLong() ?: 0,
                triggerTime = (map["triggerTime"] as? Number)?.toLong() ?: 0,
                status = map["status"] as? String ?: STATUS_SCHEDULED
            )
        }
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "instanceId" to instanceId,
        "alarmId" to alarmId,
        "triggerTime" to triggerTime,
        "status" to status
    )
}
