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

    @Query("SELECT * FROM categories WHERE `key` = :key LIMIT 1")
    suspend fun getByKey(key: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(categories: List<CategoryEntity>)

    @Upsert
    suspend fun upsert(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE `key` = :key")
    suspend fun deleteByKey(key: String)

    @Query("SELECT COUNT(*) FROM transactions WHERE category = :key")
    suspend fun countTransactionsUsing(key: String): Int

    @Query("SELECT * FROM categories WHERE synced = 0")
    suspend fun getUnsynced(): List<CategoryEntity>
}
