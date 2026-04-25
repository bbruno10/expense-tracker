package com.brunobrandao.expensetracker.domain.usecase

import com.brunobrandao.expensetracker.domain.repository.TransactionRepository
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.deleteTransactionById(id)
    }
}
