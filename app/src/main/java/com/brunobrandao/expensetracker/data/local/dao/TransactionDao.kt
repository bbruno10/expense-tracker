package com.brunobrandao.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.brunobrandao.expensetracker.data.local.entity.TransactionEntity
import com.brunobrandao.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY date DESC")
    fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE type = :type AND date BETWEEN :startDate AND :endDate
        ORDER BY date DESC
        """
    )
    fun getTransactionsByTypeAndDateRange(
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = :type")
    fun getTotalByType(type: TransactionType): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions
        WHERE type = :type AND date BETWEEN :startDate AND :endDate
        """
    )
    fun getTotalByTypeAndDateRange(
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0.0 END), 0.0) -
               COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0.0 END), 0.0)
        FROM transactions
        """
    )
    fun getBalance(): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0.0 END), 0.0) -
               COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0.0 END), 0.0)
        FROM transactions
        WHERE date BETWEEN :startDate AND :endDate
        """
    )
    fun getBalanceByDateRange(startDate: Long, endDate: Long): Flow<Double>

    // --- Sync queries ---

    @Query("SELECT * FROM transactions WHERE synced = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE remoteId = ''")
    suspend fun getTransactionsWithoutRemoteId(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): TransactionEntity?

    @Query("DELETE FROM transactions WHERE remoteId = :remoteId")
    suspend fun deleteByRemoteId(remoteId: String)

    @Query("UPDATE transactions SET remoteId = :remoteId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateRemoteId(id: Long, remoteId: String, updatedAt: Long)

    @Query("UPDATE transactions SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("SELECT * FROM transactions WHERE recurringId = :recurringId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestByRecurringId(recurringId: Long): TransactionEntity?

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
