package com.brunobrandao.expensetracker.domain.util

import com.brunobrandao.expensetracker.domain.model.RecurringFrequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class RecurringDateCalculatorTest {

    private fun utcMillis(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

    // ─── advance ──────────────────────────────────────────────────────────────

    @Test
    fun `advance WEEKLY adds 7 days`() {
        val base = utcMillis(2024, 1, 1)
        val expected = utcMillis(2024, 1, 8)
        assertEquals(expected, RecurringDateCalculator.advance(base, RecurringFrequency.WEEKLY))
    }

    @Test
    fun `advance MONTHLY adds one month`() {
        val base = utcMillis(2024, 1, 15)
        val expected = utcMillis(2024, 2, 15)
        assertEquals(expected, RecurringDateCalculator.advance(base, RecurringFrequency.MONTHLY))
    }

    @Test
    fun `advance YEARLY adds one year`() {
        val base = utcMillis(2024, 3, 10)
        val expected = utcMillis(2025, 3, 10)
        assertEquals(expected, RecurringDateCalculator.advance(base, RecurringFrequency.YEARLY))
    }

    // ─── computeNextDueDate ───────────────────────────────────────────────────

    @Test
    fun `computeNextDueDate returns startDate unchanged when startDate is in the future`() {
        val now = utcMillis(2024, 6, 1)
        val startDate = utcMillis(2024, 6, 15)
        val result = RecurringDateCalculator.computeNextDueDate(startDate, RecurringFrequency.MONTHLY, now)
        assertEquals(startDate, result)
    }

    @Test
    fun `computeNextDueDate advances past startDate equal to now`() {
        val now = utcMillis(2024, 6, 1)
        val startDate = now
        val result = RecurringDateCalculator.computeNextDueDate(startDate, RecurringFrequency.MONTHLY, now)
        // startDate == now, so must advance at least once
        assertTrue(result > now)
        assertEquals(utcMillis(2024, 7, 1), result)
    }

    @Test
    fun `computeNextDueDate skips multiple past periods MONTHLY`() {
        val startDate = utcMillis(2023, 11, 15)
        val now = utcMillis(2024, 6, 1)
        val result = RecurringDateCalculator.computeNextDueDate(startDate, RecurringFrequency.MONTHLY, now)
        assertTrue("nextDueDate must be > now", result > now)
        assertEquals(utcMillis(2024, 6, 15), result)
    }

    @Test
    fun `computeNextDueDate skips past periods WEEKLY`() {
        val startDate = utcMillis(2024, 5, 1)
        val now = utcMillis(2024, 6, 1)
        val result = RecurringDateCalculator.computeNextDueDate(startDate, RecurringFrequency.WEEKLY, now)
        assertTrue(result > now)
        // 2024-05-01 + 4 weeks = 2024-05-29, + 1 = 2024-06-05 > now(06-01)
        assertEquals(utcMillis(2024, 6, 5), result)
    }

    @Test
    fun `computeNextDueDate skips past periods YEARLY`() {
        val startDate = utcMillis(2021, 3, 10)
        val now = utcMillis(2024, 6, 1)
        val result = RecurringDateCalculator.computeNextDueDate(startDate, RecurringFrequency.YEARLY, now)
        assertTrue(result > now)
        assertEquals(utcMillis(2025, 3, 10), result)
    }

    // ─── currentOccurrenceDate ────────────────────────────────────────────────

    @Test
    fun `currentOccurrenceDate returns null when startDate is in the future`() {
        val now = utcMillis(2024, 6, 1)
        val startDate = utcMillis(2024, 6, 15)
        assertNull(RecurringDateCalculator.currentOccurrenceDate(startDate, RecurringFrequency.MONTHLY, now))
    }

    @Test
    fun `currentOccurrenceDate returns startDate when startDate equals now`() {
        val now = utcMillis(2024, 6, 1)
        val result = RecurringDateCalculator.currentOccurrenceDate(now, RecurringFrequency.MONTHLY, now)
        assertEquals(now, result)
    }

    @Test
    fun `currentOccurrenceDate MONTHLY returns most recent period not exceeding now`() {
        val startDate = utcMillis(2024, 1, 15)
        val now = utcMillis(2024, 4, 5)
        // Jan-15, Feb-15, Mar-15 are <= Apr-05; Apr-15 is not
        val result = RecurringDateCalculator.currentOccurrenceDate(startDate, RecurringFrequency.MONTHLY, now)
        assertEquals(utcMillis(2024, 3, 15), result)
    }

    @Test
    fun `currentOccurrenceDate WEEKLY returns most recent period not exceeding now`() {
        val startDate = utcMillis(2024, 1, 1)
        val now = utcMillis(2024, 1, 22)
        // Jan-01, Jan-08, Jan-15, Jan-22 are <= Jan-22; Jan-29 is not
        val result = RecurringDateCalculator.currentOccurrenceDate(startDate, RecurringFrequency.WEEKLY, now)
        assertEquals(utcMillis(2024, 1, 22), result)
    }

    @Test
    fun `currentOccurrenceDate YEARLY returns most recent period not exceeding now`() {
        val startDate = utcMillis(2021, 3, 10)
        val now = utcMillis(2024, 6, 1)
        // 2021-03-10, 2022-03-10, 2023-03-10, 2024-03-10 are <= Jun-01-2024; 2025-03-10 is not
        val result = RecurringDateCalculator.currentOccurrenceDate(startDate, RecurringFrequency.YEARLY, now)
        assertEquals(utcMillis(2024, 3, 10), result)
    }

    @Test
    fun `currentOccurrenceDate returns startDate when now is before first advance`() {
        val startDate = utcMillis(2024, 6, 1)
        val now = utcMillis(2024, 6, 10)
        // Only one period: Jun-01 <= Jun-10; Jul-01 > Jun-10
        val result = RecurringDateCalculator.currentOccurrenceDate(startDate, RecurringFrequency.MONTHLY, now)
        assertEquals(startDate, result)
    }

    @Test
    fun `computeNextDueDate invariant result is always strictly after now`() {
        val freqs = RecurringFrequency.entries
        val now = utcMillis(2024, 6, 15)
        for (freq in freqs) {
            val past = utcMillis(2020, 1, 1)
            val result = RecurringDateCalculator.computeNextDueDate(past, freq, now)
            assertTrue("Expected result > now for $freq", result > now)
        }
    }

    // ─── occurrenceAt ─────────────────────────────────────────────────────────

    @Test
    fun `occurrenceAt MONTHLY anchored on Jan-31 2025 re-anchors after February clamp`() {
        val start = utcMillis(2025, 1, 31)
        assertEquals(utcMillis(2025, 1, 31), RecurringDateCalculator.occurrenceAt(start, RecurringFrequency.MONTHLY, 0))
        assertEquals(utcMillis(2025, 2, 28), RecurringDateCalculator.occurrenceAt(start, RecurringFrequency.MONTHLY, 1))
        assertEquals(utcMillis(2025, 3, 31), RecurringDateCalculator.occurrenceAt(start, RecurringFrequency.MONTHLY, 2))
        assertEquals(utcMillis(2025, 4, 30), RecurringDateCalculator.occurrenceAt(start, RecurringFrequency.MONTHLY, 3))
        assertEquals(utcMillis(2025, 5, 31), RecurringDateCalculator.occurrenceAt(start, RecurringFrequency.MONTHLY, 4))
        assertEquals(utcMillis(2025, 6, 30), RecurringDateCalculator.occurrenceAt(start, RecurringFrequency.MONTHLY, 5))
        assertEquals(utcMillis(2025, 7, 31), RecurringDateCalculator.occurrenceAt(start, RecurringFrequency.MONTHLY, 6))
    }

    @Test
    fun `occurrenceAt MONTHLY anchored on day-30 clamps in February then re-anchors`() {
        val start = utcMillis(2025, 1, 30)
        assertEquals(utcMillis(2025, 1, 30), RecurringDateCalculator.occurrenceAt(start, RecurringFrequency.MONTHLY, 0))
        assertEquals(utcMillis(2025, 2, 28), RecurringDateCalculator.occurrenceAt(start, RecurringFrequency.MONTHLY, 1))
        assertEquals(utcMillis(2025, 3, 30), RecurringDateCalculator.occurrenceAt(start, RecurringFrequency.MONTHLY, 2))
        assertEquals(utcMillis(2025, 4, 30), RecurringDateCalculator.occurrenceAt(start, RecurringFrequency.MONTHLY, 3))
    }

    @Test
    fun `occurrenceAt YEARLY anchored on Feb-29 leap year clamps non-leap and re-expands`() {
        val start = utcMillis(2024, 2, 29)
        assertEquals(utcMillis(2024, 2, 29), RecurringDateCalculator.occurrenceAt(start, RecurringFrequency.YEARLY, 0))
        assertEquals(utcMillis(2025, 2, 28), RecurringDateCalculator.occurrenceAt(start, RecurringFrequency.YEARLY, 1))
        assertEquals(utcMillis(2026, 2, 28), RecurringDateCalculator.occurrenceAt(start, RecurringFrequency.YEARLY, 2))
        assertEquals(utcMillis(2027, 2, 28), RecurringDateCalculator.occurrenceAt(start, RecurringFrequency.YEARLY, 3))
        assertEquals(utcMillis(2028, 2, 29), RecurringDateCalculator.occurrenceAt(start, RecurringFrequency.YEARLY, 4))
    }

    @Test
    fun `computeNextDueDate with day-31 anchor does not get stuck at day 28`() {
        val start = utcMillis(2025, 1, 31)
        val now = utcMillis(2025, 3, 15)
        // Jan-31(i=0) and Feb-28(i=1) are <= Mar-15; Mar-31(i=2) > Mar-15
        val result = RecurringDateCalculator.computeNextDueDate(start, RecurringFrequency.MONTHLY, now)
        assertEquals(utcMillis(2025, 3, 31), result)
    }

    @Test
    fun `currentOccurrenceDate with day-31 anchor does not drift to day 28`() {
        val start = utcMillis(2025, 1, 31)
        val now = utcMillis(2025, 4, 5)
        // Jan-31(i=0), Feb-28(i=1), Mar-31(i=2) <= Apr-5; Apr-30(i=3) > Apr-5
        val result = RecurringDateCalculator.currentOccurrenceDate(start, RecurringFrequency.MONTHLY, now)
        assertEquals(utcMillis(2025, 3, 31), result)
    }
}
