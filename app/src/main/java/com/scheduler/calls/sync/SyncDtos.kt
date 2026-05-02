package com.scheduler.calls.sync

import com.scheduler.calls.data.Recurrence
import com.scheduler.calls.data.ScheduledCall
import com.scheduler.calls.data.ScheduledCallTime
import com.squareup.moshi.JsonClass
import java.time.LocalDateTime
import java.time.ZoneId

@JsonClass(generateAdapter = true)
data class CallDto(
    val syncId: String,
    val contactName: String,
    val phoneNumber: String,
    val localDateTime: String,
    val zoneId: String,
    val notes: String,
    val recurrence: String,
    val triggered: Boolean,
    val tombstone: Boolean,
    val updatedAt: Long
)

@JsonClass(generateAdapter = true)
data class SyncRequestDto(
    val since: Long,
    val changes: List<CallDto>
)

@JsonClass(generateAdapter = true)
data class SyncResponseDto(
    val serverTime: Long,
    val changes: List<CallDto>
)

@JsonClass(generateAdapter = true)
data class HealthDto(
    val ok: Boolean,
    val serverTime: Long
)

fun ScheduledCall.toDto(): CallDto = CallDto(
    syncId = syncId,
    contactName = contactName,
    phoneNumber = phoneNumber,
    localDateTime = if (localDateTime.length == 16) "$localDateTime:00" else localDateTime,
    zoneId = zoneId,
    notes = notes,
    recurrence = recurrence.name,
    triggered = triggered,
    tombstone = tombstone,
    updatedAt = updatedAt
)

fun CallDto.toEntity(existingId: Long = 0): ScheduledCall {
    val recur = runCatching { Recurrence.valueOf(recurrence) }.getOrDefault(Recurrence.NONE)
    val ldtNormalized = localDateTime.removeSuffix(":00").let {
        if (it.length == 16) it else localDateTime.substring(0, minOf(localDateTime.length, 19))
    }
    val zone = runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.systemDefault())
    val parsed = runCatching { LocalDateTime.parse(ldtNormalized) }
        .getOrElse { LocalDateTime.now() }
    val millis = ScheduledCallTime.computeMillis(parsed, zone)
    return ScheduledCall(
        id = existingId,
        syncId = syncId,
        contactName = contactName,
        phoneNumber = phoneNumber,
        localDateTime = parsed.toString(),
        zoneId = zone.id,
        scheduledTimeMillis = millis,
        notes = notes,
        triggered = triggered,
        recurrence = recur,
        updatedAt = updatedAt,
        tombstone = tombstone
    )
}
