package com.brunobrandao.expensetracker.domain.usecase

import com.brunobrandao.expensetracker.domain.model.Transaction
import com.brunobrandao.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTransactionsByDateRangeUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return repository.getTransactionsByDateRange(startDate, endDate)
    }
}
