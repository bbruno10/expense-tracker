package com.brunobrandao.expensetracker.domain.usecase

import com.brunobrandao.expensetracker.domain.model.Transaction
import com.brunobrandao.expensetracker.domain.repository.RecurringTransactionRepository
import com.brunobrandao.expensetracker.domain.repository.TransactionRepository
import com.brunobrandao.expensetracker.domain.util.RecurringDateCalculator
import javax.inject.Inject

class GenerateDueRecurringTransactionsUseCase @Inject constructor(
    private val recurringRepository: RecurringTransactionRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(now: Long) {
        val dueRules = recurringRepository.getActiveDue(now)
        for (rule in dueRules) {
            val start = rule.startDate
            var index = 0
            while (RecurringDateCalculator.occurrenceAt(start, rule.frequency, index) < rule.nextDueDate) index++
            var nextDue = RecurringDateCalculator.occurrenceAt(start, rule.frequency, index)
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
                index++
                nextDue = RecurringDateCalculator.occurrenceAt(start, rule.frequency, index)
            }
            recurringRepository.upsert(rule.copy(nextDueDate = nextDue))
        }
    }
}
