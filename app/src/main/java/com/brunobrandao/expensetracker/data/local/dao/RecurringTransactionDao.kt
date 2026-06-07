package com.brunobrandao.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.brunobrandao.expensetracker.data.local.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecurringTransactionEntity): Long

    @Update
    suspend fun update(entity: RecurringTransactionEntity)

    @Delete
    suspend fun delete(entity: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM recurring_transactions ORDER BY nextDueDate ASC")
    fun getAll(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getById(id: Long): RecurringTransactionEntity?

    @Query("SELECT * FROM recurring_transactions WHERE active = 1 AND nextDueDate <= :now")
    suspend fun getActiveDue(now: Long): List<RecurringTransactionEntity>

    // --- Sync queries ---

    @Query("SELECT * FROM recurring_transactions WHERE synced = 0")
    suspend fun getUnsyncedTransactions(): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): RecurringTransactionEntity?

    @Query("UPDATE recurring_transactions SET remoteId = :remoteId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateRemoteId(id: Long, remoteId: String, updatedAt: Long)

    @Query("UPDATE recurring_transactions SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)
}
