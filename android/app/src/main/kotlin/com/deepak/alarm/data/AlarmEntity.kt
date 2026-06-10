package com.deepak.alarm.data

/**
 * Data class representing an alarm definition.
 * repeatDays is a bitmask: bit 0 = Monday ... bit 6 = Sunday
 */
data class AlarmEntity(
    val id: Long = 0,
    val label: String = "",
    val hour: Int = 0,
    val minute: Int = 0,
    val enabled: Boolean = true,
    val repeatDays: Int = 0,
    val soundUri: String = "",
    val vibrationEnabled: Boolean = true,
    val snoozeMinutes: Int = 10,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val MONDAY    = 1 shl 0  // 1
        const val TUESDAY   = 1 shl 1  // 2
        const val WEDNESDAY = 1 shl 2  // 4
        const val THURSDAY  = 1 shl 3  // 8
        const val FRIDAY    = 1 shl 4  // 16
        const val SATURDAY  = 1 shl 5  // 32
        const val SUNDAY    = 1 shl 6  // 64

        fun fromMap(map: Map<String, Any?>): AlarmEntity {
            return AlarmEntity(
                id = (map["id"] as? Number)?.toLong() ?: 0,
                label = map["label"] as? String ?: "",
                hour = (map["hour"] as? Number)?.toInt() ?: 0,
                minute = (map["minute"] as? Number)?.toInt() ?: 0,
                enabled = map["enabled"] as? Boolean ?: true,
                repeatDays = (map["repeatDays"] as? Number)?.toInt() ?: 0,
                soundUri = map["soundUri"] as? String ?: "",
                vibrationEnabled = map["vibrationEnabled"] as? Boolean ?: true,
                snoozeMinutes = (map["snoozeMinutes"] as? Number)?.toInt() ?: 10,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "label" to label,
        "hour" to hour,
        "minute" to minute,
        "enabled" to enabled,
        "repeatDays" to repeatDays,
        "soundUri" to soundUri,
        "vibrationEnabled" to vibrationEnabled,
        "snoozeMinutes" to snoozeMinutes,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    fun isRepeating(): Boolean = repeatDays != 0

    fun repeatsOnDay(dayBit: Int): Boolean = (repeatDays and dayBit) != 0
}
