package com.scheduler.calls.ui.util

import androidx.compose.ui.graphics.Color

object Avatar {

    private val palette = listOf(
        Color(0xFF6650A4),
        Color(0xFF005EB8),
        Color(0xFF1E8E3E),
        Color(0xFFB36B00),
        Color(0xFFB3261E),
        Color(0xFF7A4DAF),
        Color(0xFF00838F),
        Color(0xFF5D4037),
        Color(0xFF455A64)
    )

    fun initials(name: String, fallbackPhone: String): String {
        val source = name.ifBlank { fallbackPhone }.trim()
        if (source.isEmpty()) return "?"
        val parts = source.split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
        }
    }

    fun colorFor(seed: String): Color {
        if (seed.isEmpty()) return palette[0]
        val hash = seed.fold(0) { acc, c -> (acc * 31 + c.code) and 0x7FFFFFFF }
        return palette[hash % palette.size]
    }
}
