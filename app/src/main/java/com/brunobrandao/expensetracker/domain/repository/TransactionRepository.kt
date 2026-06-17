package com.brunobrandao.expensetracker.domain.repository

import com.brunobrandao.expensetracker.domain.model.Transaction
import com.brunobrandao.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {

    fun getAllTransactions(): Flow<List<Transaction>>

    suspend fun getTransactionById(id: Long): Transaction?

    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>

    fun getTransactionsByCategory(category: String): Flow<List<Transaction>>

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>

    fun getTransactionsByTypeAndDateRange(
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>>

    fun getTotalByType(type: TransactionType): Flow<Double>

    fun getTotalByTypeAndDateRange(
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Flow<Double>

    fun getBalance(): Flow<Double>

    fun getBalanceByDateRange(startDate: Long, endDate: Long): Flow<Double>

    suspend fun insertTransaction(transaction: Transaction): Long

    suspend fun updateTransaction(transaction: Transaction)

    suspend fun deleteTransaction(transaction: Transaction)

    suspend fun deleteTransactionById(id: Long)
}
