package com.scheduler.calls.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.ZoneId

@Entity(tableName = "scheduled_calls")
data class ScheduledCall(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactName: String,
    val phoneNumber: String,
    val localDateTime: String,
    val zoneId: String,
    val scheduledTimeMillis: Long,
    val notes: String,
    val triggered: Boolean = false,
    val recurrence: Recurrence = Recurrence.NONE
) {
    fun parsedLocalDateTime(): LocalDateTime = LocalDateTime.parse(localDateTime)
    fun zone(): ZoneId = runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.systemDefault())
}

object ScheduledCallTime {
    fun computeMillis(local: LocalDateTime, zone: ZoneId): Long =
        local.atZone(zone).toInstant().toEpochMilli()
}
