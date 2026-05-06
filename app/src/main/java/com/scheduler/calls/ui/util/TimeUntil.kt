package com.scheduler.calls.ui.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

object TimeUntil {

    private val timeFmt = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val sameYearDateFmt = DateTimeFormatter.ofPattern("MMM d • h:mm a", Locale.getDefault())
    private val otherYearDateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    private val weekdayFmt = DateTimeFormatter.ofPattern("EEE • h:mm a", Locale.getDefault())

    /** Human-friendly relative label, e.g. "in 25m", "tomorrow 8:00 AM", "Mon 8:00 AM", "May 20". */
    fun relative(targetMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diff = targetMillis - nowMillis
        val zone = ZoneId.systemDefault()
        val target = java.time.Instant.ofEpochMilli(targetMillis).atZone(zone).toLocalDateTime()
        val now = java.time.Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDateTime()
        val today = LocalDate.now(zone)
        val targetDate = target.toLocalDate()

        return when {
            diff in 0 until 60_000 -> "now"
            diff in 0 until 3_600_000 -> "in ${diff / 60_000}m"
            diff in 0 until 24 * 3_600_000L -> "in ${diff / 3_600_000}h"
            targetDate == today.plusDays(1) -> "tomorrow ${target.toLocalTime().format(timeFmt)}"
            targetDate.isAfter(today) && targetDate.isBefore(today.plusDays(7)) ->
                target.format(weekdayFmt)
            targetDate.year == now.year -> target.format(sameYearDateFmt)
            else -> target.format(otherYearDateFmt)
        }.let { base ->
            if (diff < 0 && abs(diff) >= 60_000) "$base (past)" else base
        }
    }
}
