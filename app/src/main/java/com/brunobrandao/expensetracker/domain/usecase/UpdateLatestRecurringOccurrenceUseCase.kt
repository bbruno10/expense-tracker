package com.brunobrandao.expensetracker.domain.usecase

import com.brunobrandao.expensetracker.domain.model.RecurringTransaction
import com.brunobrandao.expensetracker.domain.repository.TransactionRepository
import com.brunobrandao.expensetracker.domain.util.Clock
import com.brunobrandao.expensetracker.domain.util.RecurringDateCalculator
import javax.inject.Inject

class UpdateLatestRecurringOccurrenceUseCase @Inject constructor(
    private val repository: TransactionRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(rule: RecurringTransaction): Long? {
        val occurrence = repository.getLatestByRecurringId(rule.id) ?: return null
        val newDate = RecurringDateCalculator.currentOccurrenceDate(rule.startDate, rule.frequency, clock.now())
        val updated = occurrence.copy(
            description = rule.description,
            amount = rule.amount,
            type = rule.type,
            category = rule.category,
            note = rule.note,
            date = newDate ?: occurrence.date
        )
        repository.updateTransaction(updated)
        return updated.id
    }
}
