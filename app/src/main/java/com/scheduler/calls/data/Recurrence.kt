package com.scheduler.calls.data

import android.content.Context
import com.scheduler.calls.R
import java.time.DayOfWeek
import java.time.LocalDateTime

enum class Recurrence(val labelRes: Int) {
    NONE(R.string.recurrence_none),
    DAILY(R.string.recurrence_daily),
    WEEKDAYS(R.string.recurrence_weekdays),
    WEEKLY(R.string.recurrence_weekly);

    fun label(context: Context): String = context.getString(labelRes)

    fun nextOccurrence(from: LocalDateTime): LocalDateTime? = when (this) {
        NONE -> null
        DAILY -> from.plusDays(1)
        WEEKLY -> from.plusWeeks(1)
        WEEKDAYS -> {
            var next = from.plusDays(1)
            while (next.dayOfWeek == DayOfWeek.SATURDAY || next.dayOfWeek == DayOfWeek.SUNDAY) {
                next = next.plusDays(1)
            }
            next
        }
    }
}
