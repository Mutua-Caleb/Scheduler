package com.scheduler.calls.data

import java.util.Calendar

enum class Recurrence(val label: String) {
    NONE("Once"),
    DAILY("Every day"),
    WEEKDAYS("Weekdays"),
    WEEKLY("Every week");

    fun nextOccurrence(fromMillis: Long): Long? {
        if (this == NONE) return null
        val cal = Calendar.getInstance().apply { timeInMillis = fromMillis }
        when (this) {
            DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            WEEKLY -> cal.add(Calendar.DAY_OF_YEAR, 7)
            WEEKDAYS -> {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                    cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                ) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            NONE -> Unit
        }
        return cal.timeInMillis
    }
}
