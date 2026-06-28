package com.brunobrandao.expensetracker.domain.usecase

import com.brunobrandao.expensetracker.domain.model.Transaction
import com.brunobrandao.expensetracker.domain.repository.RecurringTransactionRepository
import com.brunobrandao.expensetracker.domain.repository.TransactionRepository
import com.brunobrandao.expensetracker.domain.util.Clock
import com.brunobrandao.expensetracker.domain.util.RecurringDateCalculator
import javax.inject.Inject

class UpdateRuleFromOccurrenceUseCase @Inject constructor(
    private val recurringRepository: RecurringTransactionRepository,
    private val transactionRepository: TransactionRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(transaction: Transaction): Long? {
        val recurringId = transaction.recurringId ?: return null
        val latest = transactionRepository.getLatestByRecurringId(recurringId)
        if (latest == null || latest.id != transaction.id) return null
        val rule = recurringRepository.getById(recurringId) ?: return null
        val updated = rule.copy(
            description = transaction.description,
            amount = transaction.amount,
            type = transaction.type,
            category = transaction.category,
            note = transaction.note,
            startDate = transaction.date,
            nextDueDate = RecurringDateCalculator.computeNextDueDate(
                transaction.date, rule.frequency, clock.now()
            )
        )
        recurringRepository.upsert(updated)
        return rule.id
    }
}
