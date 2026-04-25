package com.brunobrandao.expensetracker.domain.usecase

import com.brunobrandao.expensetracker.domain.model.TransactionType
import com.brunobrandao.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTotalByTypeUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(type: TransactionType): Flow<Double> {
        return repository.getTotalByType(type)
    }

    fun byDateRange(type: TransactionType, startDate: Long, endDate: Long): Flow<Double> {
        return repository.getTotalByTypeAndDateRange(type, startDate, endDate)
    }
}
