package com.brunobrandao.expensetracker.data.repository

import com.brunobrandao.expensetracker.data.local.dao.RecurringTransactionDao
import com.brunobrandao.expensetracker.data.local.entity.toDomain
import com.brunobrandao.expensetracker.data.local.entity.toEntity
import com.brunobrandao.expensetracker.domain.model.RecurringTransaction
import com.brunobrandao.expensetracker.domain.repository.RecurringTransactionRepository
import com.brunobrandao.expensetracker.domain.util.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringTransactionRepositoryImpl @Inject constructor(
    private val dao: RecurringTransactionDao,
    private val clock: Clock
) : RecurringTransactionRepository {

    override fun observeAll(): Flow<List<RecurringTransaction>> {
        return dao.getAll().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getById(id: Long): RecurringTransaction? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun getActiveDue(now: Long): List<RecurringTransaction> {
        return dao.getActiveDue(now).map { it.toDomain() }
    }

    override suspend fun upsert(rule: RecurringTransaction): Long {
        return if (rule.id == 0L) {
            dao.insert(rule.toEntity())
        } else {
            val existing = dao.getById(rule.id)
            val entity = rule.toEntity().copy(
                remoteId = existing?.remoteId ?: "",
                synced = false,
                updatedAt = clock.now()
            )
            dao.update(entity)
            rule.id
        }
    }

    override suspend fun delete(rule: RecurringTransaction) {
        dao.delete(rule.toEntity())
    }

    override suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun setActive(id: Long, active: Boolean) {
        val existing = dao.getById(id) ?: return
        dao.update(
            existing.copy(
                active = active,
                synced = false,
                updatedAt = clock.now()
            )
        )
    }
}
