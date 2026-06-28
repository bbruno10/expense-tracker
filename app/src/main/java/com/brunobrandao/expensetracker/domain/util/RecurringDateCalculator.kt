package com.brunobrandao.expensetracker.domain.util

import com.brunobrandao.expensetracker.domain.model.RecurringFrequency
import java.time.Instant
import java.time.ZoneOffset

object RecurringDateCalculator {

    fun occurrenceAt(startDate: Long, frequency: RecurringFrequency, index: Int): Long {
        val date = Instant.ofEpochMilli(startDate)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
        val result = when (frequency) {
            RecurringFrequency.WEEKLY  -> date.plusWeeks(index.toLong())
            RecurringFrequency.MONTHLY -> date.plusMonths(index.toLong())
            RecurringFrequency.YEARLY  -> date.plusYears(index.toLong())
        }
        return result.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    // Single-step helper. Do NOT call iteratively — accumulated month-end clamp
    // causes day drift (Jan-31 → Feb-28 → Mar-28 → …). Use occurrenceAt() instead.
    fun advance(epochMillis: Long, frequency: RecurringFrequency): Long {
        val date = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
        val advanced = when (frequency) {
            RecurringFrequency.WEEKLY  -> date.plusWeeks(1)
            RecurringFrequency.MONTHLY -> date.plusMonths(1)
            RecurringFrequency.YEARLY  -> date.plusYears(1)
        }
        return advanced.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    /**
     * Returns the most recent scheduled date (<= [now]) derived from [startDate] + N*[frequency].
     * Returns null when [startDate] is in the future (no occurrence has happened yet).
     */
    fun currentOccurrenceDate(startDate: Long, frequency: RecurringFrequency, now: Long): Long? {
        if (startDate > now) return null
        var i = 0
        while (occurrenceAt(startDate, frequency, i + 1) <= now) i++
        return occurrenceAt(startDate, frequency, i)
    }

    /**
     * Returns the first occurrence of [startDate] + N*[frequency] that is strictly after [now].
     * If [startDate] is already in the future, returns [startDate] unchanged.
     * Opção A: always future — no catch-up on creation or update.
     */
    fun computeNextDueDate(startDate: Long, frequency: RecurringFrequency, now: Long): Long {
        var i = 0
        while (occurrenceAt(startDate, frequency, i) <= now) i++
        return occurrenceAt(startDate, frequency, i)
    }
}
