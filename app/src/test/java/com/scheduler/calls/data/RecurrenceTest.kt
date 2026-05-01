package com.scheduler.calls.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class RecurrenceTest {

    @Test
    fun `NONE never recurs`() {
        assertNull(Recurrence.NONE.nextOccurrence(LocalDateTime.of(2026, 5, 1, 8, 0)))
    }

    @Test
    fun `DAILY adds one day`() {
        val from = LocalDateTime.of(2026, 5, 1, 8, 0)
        val next = Recurrence.DAILY.nextOccurrence(from)
        assertEquals(LocalDateTime.of(2026, 5, 2, 8, 0), next)
    }

    @Test
    fun `WEEKLY adds seven days`() {
        val from = LocalDateTime.of(2026, 5, 1, 8, 0)
        val next = Recurrence.WEEKLY.nextOccurrence(from)
        assertEquals(LocalDateTime.of(2026, 5, 8, 8, 0), next)
    }

    @Test
    fun `WEEKDAYS skips Saturday and Sunday`() {
        // 2026-05-01 is a Friday. Next weekday should be Monday 2026-05-04.
        val friday = LocalDateTime.of(2026, 5, 1, 8, 0)
        val next = Recurrence.WEEKDAYS.nextOccurrence(friday)
        assertEquals(LocalDateTime.of(2026, 5, 4, 8, 0), next)
    }

    @Test
    fun `WEEKDAYS from Saturday lands on Monday`() {
        val saturday = LocalDateTime.of(2026, 5, 2, 8, 0)
        val next = Recurrence.WEEKDAYS.nextOccurrence(saturday)
        assertEquals(LocalDateTime.of(2026, 5, 4, 8, 0), next)
    }

    @Test
    fun `DAILY across DST spring-forward keeps wall-clock time`() {
        // US DST 2026: spring-forward at 2026-03-08 02:00 local in America/Los_Angeles.
        // 8 AM the day before should still produce 8 AM the next day in the same zone,
        // even though the absolute UTC delta is 23h instead of 24h.
        val zone = ZoneId.of("America/Los_Angeles")
        val before = LocalDateTime.of(2026, 3, 7, 8, 0)
        val after = Recurrence.DAILY.nextOccurrence(before)!!
        assertEquals(LocalDateTime.of(2026, 3, 8, 8, 0), after)

        val beforeMillis = ScheduledCallTime.computeMillis(before, zone)
        val afterMillis = ScheduledCallTime.computeMillis(after, zone)
        // 23-hour gap because of spring-forward.
        assertEquals(23L * 60 * 60 * 1000, afterMillis - beforeMillis)
    }

    @Test
    fun `computeMillis honors stored zone, not system zone`() {
        // 8 AM in Los Angeles is 11 AM in New York — same wall clock,
        // different zones, different instants.
        val local = LocalDateTime.of(2026, 5, 1, 8, 0)
        val laMillis = ScheduledCallTime.computeMillis(local, ZoneId.of("America/Los_Angeles"))
        val nyMillis = ScheduledCallTime.computeMillis(local, ZoneId.of("America/New_York"))
        assertEquals(3L * 60 * 60 * 1000, laMillis - nyMillis)
    }
}
