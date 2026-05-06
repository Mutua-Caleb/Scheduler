package com.scheduler.calls.ui

import com.scheduler.calls.R
import com.scheduler.calls.data.Recurrence
import com.scheduler.calls.data.ScheduledCall
import java.time.LocalDate
import java.time.ZoneId

enum class CallFilter(val labelRes: Int) {
    ALL(R.string.filter_all),
    TODAY(R.string.filter_today),
    THIS_WEEK(R.string.filter_this_week),
    RECURRING(R.string.filter_recurring),
    TRIGGERED(R.string.filter_triggered);

    fun matches(call: ScheduledCall, nowMillis: Long): Boolean {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val startOfToday = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val startOfTomorrow = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val sevenDaysOut = nowMillis + 7L * 24 * 60 * 60 * 1000

        return when (this) {
            ALL -> true
            TODAY -> call.scheduledTimeMillis in startOfToday until startOfTomorrow
            THIS_WEEK -> call.scheduledTimeMillis in nowMillis..sevenDaysOut
            RECURRING -> call.recurrence != Recurrence.NONE
            TRIGGERED -> call.triggered
        }
    }
}

fun applyFilter(
    calls: List<ScheduledCall>,
    query: String,
    filter: CallFilter,
    now: Long = System.currentTimeMillis()
): List<ScheduledCall> {
    val needle = query.trim().lowercase()
    return calls.asSequence()
        .filter { filter.matches(it, now) }
        .filter {
            needle.isEmpty() ||
                it.contactName.lowercase().contains(needle) ||
                it.phoneNumber.lowercase().contains(needle) ||
                it.notes.lowercase().contains(needle)
        }
        .toList()
}
