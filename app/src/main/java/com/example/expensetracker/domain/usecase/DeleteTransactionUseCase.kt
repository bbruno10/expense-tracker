package com.example.expensetracker.domain.usecase

import com.example.expensetracker.domain.repository.TransactionRepository
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.deleteTransactionById(id)
    }
}
