package com.brunobrandao.expensetracker.domain.util

import com.brunobrandao.expensetracker.domain.model.RecurringFrequency
import java.time.Instant
import java.time.ZoneOffset

object RecurringDateCalculator {

    // TODO: MONTHLY edge cases — LocalDate.plusMonths() clamps to last valid day (Jan-31 → Feb-28/29),
    //       causing day drift (Feb-28 → Mar-28 → ...). Fix in a separate PR.

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
        var current = startDate
        while (true) {
            val next = advance(current, frequency)
            if (next > now) return current
            current = next
        }
    }

    /**
     * Returns the first occurrence of [startDate] + N*[frequency] that is strictly after [now].
     * If [startDate] is already in the future, returns [startDate] unchanged.
     * Opção A: always future — no catch-up on creation or update.
     */
    fun computeNextDueDate(startDate: Long, frequency: RecurringFrequency, now: Long): Long {
        var next = startDate
        while (next <= now) {
            next = advance(next, frequency)
        }
        return next
    }
}
