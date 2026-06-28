package com.brunobrandao.expensetracker.data.repository

import com.brunobrandao.expensetracker.data.local.dao.TransactionDao
import com.brunobrandao.expensetracker.data.local.entity.toDomain
import com.brunobrandao.expensetracker.data.local.entity.toEntity
import com.brunobrandao.expensetracker.domain.model.Transaction
import com.brunobrandao.expensetracker.domain.model.TransactionType
import com.brunobrandao.expensetracker.domain.repository.TransactionRepository
import com.brunobrandao.expensetracker.domain.util.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao,
    private val clock: Clock
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return dao.getAllTransactions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return dao.getTransactionById(id)?.toDomain()
    }

    override fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> {
        return dao.getTransactionsByType(type).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTransactionsByCategory(category: String): Flow<List<Transaction>> {
        return dao.getTransactionsByCategory(category).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return dao.getTransactionsByDateRange(startDate, endDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTransactionsByTypeAndDateRange(
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>> {
        return dao.getTransactionsByTypeAndDateRange(type, startDate, endDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTotalByType(type: TransactionType): Flow<Double> {
        return dao.getTotalByType(type)
    }

    override fun getTotalByTypeAndDateRange(
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Flow<Double> {
        return dao.getTotalByTypeAndDateRange(type, startDate, endDate)
    }

    override fun getBalance(): Flow<Double> {
        return dao.getBalance()
    }

    override fun getBalanceByDateRange(startDate: Long, endDate: Long): Flow<Double> {
        return dao.getBalanceByDateRange(startDate, endDate)
    }

    override suspend fun getLatestByRecurringId(recurringId: Long): Transaction? {
        return dao.getLatestByRecurringId(recurringId)?.toDomain()
    }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        return dao.insert(transaction.toEntity())
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        val existing = dao.getTransactionById(transaction.id)
        val entity = transaction.toEntity().copy(
            remoteId = existing?.remoteId ?: "",
            synced = false,
            updatedAt = clock.now()
        )
        dao.update(entity)
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        dao.delete(transaction.toEntity())
    }

    override suspend fun deleteTransactionById(id: Long) {
        dao.deleteById(id)
    }
}
