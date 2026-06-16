package com.brunobrandao.expensetracker.domain.usecase

import com.brunobrandao.expensetracker.domain.model.RecurringFrequency
import com.brunobrandao.expensetracker.domain.model.Transaction
import com.brunobrandao.expensetracker.domain.repository.RecurringTransactionRepository
import com.brunobrandao.expensetracker.domain.repository.TransactionRepository
import java.time.ZoneOffset
import javax.inject.Inject

class GenerateDueRecurringTransactionsUseCase @Inject constructor(
    private val recurringRepository: RecurringTransactionRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(now: Long) {
        val dueRules = recurringRepository.getActiveDue(now)
        for (rule in dueRules) {
            var nextDue = rule.nextDueDate
            while (nextDue <= now) {
                transactionRepository.insertTransaction(
                    Transaction(
                        description = rule.description,
                        amount = rule.amount,
                        type = rule.type,
                        category = rule.category,
                        note = rule.note,
                        date = nextDue,
                        recurringId = rule.id
                    )
                )
                nextDue = advanceDate(nextDue, rule.frequency)
            }
            recurringRepository.upsert(rule.copy(nextDueDate = nextDue))
        }
    }

    private fun advanceDate(epochMillis: Long, frequency: RecurringFrequency): Long {
        val date = java.time.Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
        val advanced = when (frequency) {
            RecurringFrequency.WEEKLY  -> date.plusWeeks(1)
            RecurringFrequency.MONTHLY -> date.plusMonths(1)
            RecurringFrequency.YEARLY  -> date.plusYears(1)
        }
        return advanced.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
}
