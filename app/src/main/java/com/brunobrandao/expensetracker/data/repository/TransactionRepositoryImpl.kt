package com.brunobrandao.expensetracker.data.repository

import com.brunobrandao.expensetracker.data.local.dao.TransactionDao
import com.brunobrandao.expensetracker.data.local.entity.toDomain
import com.brunobrandao.expensetracker.data.local.entity.toEntity
import com.brunobrandao.expensetracker.domain.model.Category
import com.brunobrandao.expensetracker.domain.model.Transaction
import com.brunobrandao.expensetracker.domain.model.TransactionType
import com.brunobrandao.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao
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

    override fun getTransactionsByCategory(category: Category): Flow<List<Transaction>> {
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

    override suspend fun insertTransaction(transaction: Transaction): Long {
        return dao.insert(transaction.toEntity())
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        // Preserve existing sync fields so remoteId and sync state are not lost on edit.
        val existing = dao.getTransactionById(transaction.id)
        val entity = transaction.toEntity().copy(
            remoteId = existing?.remoteId ?: "",
            synced = false,
            updatedAt = System.currentTimeMillis()
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
