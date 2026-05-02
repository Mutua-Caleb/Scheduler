package com.scheduler.calls.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@Entity(
    tableName = "scheduled_calls",
    indices = [Index(value = ["syncId"], unique = true)]
)
data class ScheduledCall(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val syncId: String = UUID.randomUUID().toString(),
    val contactName: String,
    val phoneNumber: String,
    val localDateTime: String,
    val zoneId: String,
    val scheduledTimeMillis: Long,
    val notes: String,
    val triggered: Boolean = false,
    val recurrence: Recurrence = Recurrence.NONE,
    val updatedAt: Long = System.currentTimeMillis(),
    val tombstone: Boolean = false
) {
    fun parsedLocalDateTime(): LocalDateTime = LocalDateTime.parse(localDateTime)
    fun zone(): ZoneId = runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.systemDefault())
}

object ScheduledCallTime {
    fun computeMillis(local: LocalDateTime, zone: ZoneId): Long =
        local.atZone(zone).toInstant().toEpochMilli()
}
