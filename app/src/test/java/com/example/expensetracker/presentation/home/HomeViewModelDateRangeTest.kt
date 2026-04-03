package com.example.expensetracker.presentation.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class HomeViewModelDateRangeTest {

    // ---- getDateRange ----

    @Test
    fun `getDateRange MONTHLY offset 0 returns current month boundaries`() {
        val (start, end) = HomeViewModel.getDateRange(TimePeriod.MONTHLY, 0)

        val calStart = Calendar.getInstance().apply { timeInMillis = start }
        val calEnd = Calendar.getInstance().apply { timeInMillis = end }
        val now = Calendar.getInstance()

        // Start should be first day of current month at midnight
        assertEquals(1, calStart.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, calStart.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calStart.get(Calendar.MINUTE))
        assertEquals(0, calStart.get(Calendar.SECOND))
        assertEquals(now.get(Calendar.MONTH), calStart.get(Calendar.MONTH))
        assertEquals(now.get(Calendar.YEAR), calStart.get(Calendar.YEAR))

        // End should be last millisecond of the month
        assertTrue(end > start)
        // End + 1 should be first day of next month
        val calNextMonth = Calendar.getInstance().apply { timeInMillis = end + 1 }
        assertEquals(1, calNextMonth.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `getDateRange MONTHLY offset -1 returns previous month`() {
        val (start, _) = HomeViewModel.getDateRange(TimePeriod.MONTHLY, -1)

        val calStart = Calendar.getInstance().apply { timeInMillis = start }
        val expected = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }

        assertEquals(expected.get(Calendar.MONTH), calStart.get(Calendar.MONTH))
        assertEquals(expected.get(Calendar.YEAR), calStart.get(Calendar.YEAR))
        assertEquals(1, calStart.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `getDateRange WEEKLY offset 0 returns 7 day range`() {
        val (start, end) = HomeViewModel.getDateRange(TimePeriod.WEEKLY, 0)

        val durationMillis = end - start + 1
        val durationDays = durationMillis / (24 * 60 * 60 * 1000)

        assertEquals(7, durationDays)
    }

    @Test
    fun `getDateRange YEARLY offset 0 returns current year boundaries`() {
        val (start, end) = HomeViewModel.getDateRange(TimePeriod.YEARLY, 0)

        val calStart = Calendar.getInstance().apply { timeInMillis = start }
        val calEnd = Calendar.getInstance().apply { timeInMillis = end }
        val now = Calendar.getInstance()

        assertEquals(now.get(Calendar.YEAR), calStart.get(Calendar.YEAR))
        assertEquals(1, calStart.get(Calendar.DAY_OF_YEAR))

        // End + 1 should be Jan 1 of next year
        val calNextYear = Calendar.getInstance().apply { timeInMillis = end + 1 }
        assertEquals(now.get(Calendar.YEAR) + 1, calNextYear.get(Calendar.YEAR))
        assertEquals(1, calNextYear.get(Calendar.DAY_OF_YEAR))
    }

    @Test
    fun `getDateRange start is always before end`() {
        TimePeriod.entries.forEach { period ->
            for (offset in -3..0) {
                val (start, end) = HomeViewModel.getDateRange(period, offset)
                assertTrue(
                    "Start ($start) should be before end ($end) for $period offset=$offset",
                    start < end
                )
            }
        }
    }

    @Test
    fun `getDateRange consecutive months do not overlap`() {
        val (_, endPrevious) = HomeViewModel.getDateRange(TimePeriod.MONTHLY, -1)
        val (startCurrent, _) = HomeViewModel.getDateRange(TimePeriod.MONTHLY, 0)

        assertTrue(
            "Previous month end ($endPrevious) should be before current month start ($startCurrent)",
            endPrevious < startCurrent
        )
    }

    @Test
    fun `getDateRange consecutive weeks do not overlap`() {
        val (_, endPrevious) = HomeViewModel.getDateRange(TimePeriod.WEEKLY, -1)
        val (startCurrent, _) = HomeViewModel.getDateRange(TimePeriod.WEEKLY, 0)

        assertTrue(
            "Previous week end ($endPrevious) should be before current week start ($startCurrent)",
            endPrevious < startCurrent
        )
    }

    // ---- getPeriodLabel ----

    @Test
    fun `getPeriodLabel MONTHLY offset 0 contains current month name`() {
        val label = HomeViewModel.getPeriodLabel(TimePeriod.MONTHLY, 0)
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR).toString()

        assertTrue("Label '$label' should contain the year $year", label.contains(year))
        assertTrue("Label '$label' should not be empty", label.isNotEmpty())
    }

    @Test
    fun `getPeriodLabel YEARLY offset 0 returns current year`() {
        val label = HomeViewModel.getPeriodLabel(TimePeriod.YEARLY, 0)
        val expectedYear = Calendar.getInstance().get(Calendar.YEAR).toString()

        assertEquals(expectedYear, label)
    }

    @Test
    fun `getPeriodLabel YEARLY offset -1 returns previous year`() {
        val label = HomeViewModel.getPeriodLabel(TimePeriod.YEARLY, -1)
        val expectedYear = (Calendar.getInstance().get(Calendar.YEAR) - 1).toString()

        assertEquals(expectedYear, label)
    }

    @Test
    fun `getPeriodLabel WEEKLY offset 0 contains dash separator`() {
        val label = HomeViewModel.getPeriodLabel(TimePeriod.WEEKLY, 0)

        assertTrue("Weekly label '$label' should contain ' - '", label.contains(" - "))
    }

    @Test
    fun `getPeriodLabel returns non-empty for all periods and offsets`() {
        TimePeriod.entries.forEach { period ->
            for (offset in -3..0) {
                val label = HomeViewModel.getPeriodLabel(period, offset)
                assertTrue(
                    "Label should not be empty for $period offset=$offset",
                    label.isNotBlank()
                )
            }
        }
    }
}
