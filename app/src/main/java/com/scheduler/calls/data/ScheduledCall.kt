package com.scheduler.calls.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_calls")
data class ScheduledCall(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactName: String,
    val phoneNumber: String,
    val scheduledTimeMillis: Long,
    val notes: String,
    val triggered: Boolean = false
)
