package com.brunobrandao.expensetracker.domain.repository

import com.brunobrandao.expensetracker.domain.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow

interface RecurringTransactionRepository {

    fun observeAll(): Flow<List<RecurringTransaction>>

    suspend fun getById(id: Long): RecurringTransaction?

    suspend fun getActiveDue(now: Long): List<RecurringTransaction>

    /** Insert se id == 0, update se id != 0. Retorna o id persistido. */
    suspend fun upsert(rule: RecurringTransaction): Long

    suspend fun delete(rule: RecurringTransaction)

    suspend fun deleteById(id: Long)

    suspend fun setActive(id: Long, active: Boolean)
}
