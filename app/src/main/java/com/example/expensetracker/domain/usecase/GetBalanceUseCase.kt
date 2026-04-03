package com.example.expensetracker.domain.usecase

import com.example.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBalanceUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<Double> {
        return repository.getBalance()
    }

    fun byDateRange(startDate: Long, endDate: Long): Flow<Double> {
        return repository.getBalanceByDateRange(startDate, endDate)
    }
}
