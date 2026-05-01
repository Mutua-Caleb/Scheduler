package com.scheduler.calls.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CallOutcome { CALLED, SNOOZED, CANCELLED, AUTO_FIRED }

@Entity(tableName = "call_events")
data class CallEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scheduledCallId: Long,
    val contactName: String,
    val phoneNumber: String,
    val firedAtMillis: Long,
    val outcome: CallOutcome,
    val notes: String
)
