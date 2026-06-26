package com.brunobrandao.expensetracker.domain.util

import com.brunobrandao.expensetracker.domain.model.RecurringFrequency
import org.junit.Assert.assertEquals
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
}
