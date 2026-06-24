package com.brunobrandao.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.brunobrandao.expensetracker.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY position ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE archived = 0 ORDER BY position ASC")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE archived = 0 ORDER BY position ASC")
    suspend fun getActive(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE `key` = :key LIMIT 1")
    suspend fun getByKey(key: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(categories: List<CategoryEntity>)

    @Upsert
    suspend fun upsert(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE `key` = :key")
    suspend fun deleteByKey(key: String)

    // Covers both transactions and recurring_transactions — guards the delete use case correctly.
    @Query("""
        SELECT (SELECT COUNT(*) FROM transactions WHERE category = :key) +
               (SELECT COUNT(*) FROM recurring_transactions WHERE category = :key)
    """)
    suspend fun countTransactionsUsing(key: String): Int

    @Query("SELECT * FROM categories WHERE synced = 0")
    suspend fun getUnsynced(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): CategoryEntity?

    @Query("UPDATE categories SET synced = 1 WHERE `key` = :key")
    suspend fun markSynced(key: String)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}
