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
                nextDue = RecurringDateCalculator.advance(nextDue, rule.frequency)
            }
            recurringRepository.upsert(rule.copy(nextDueDate = nextDue))
        }
    }
}
