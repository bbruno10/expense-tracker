package com.brunobrandao.expensetracker.domain.repository

import com.brunobrandao.expensetracker.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    fun observeCategories(): Flow<List<Category>>

    fun observeActiveCategories(): Flow<List<Category>>

    suspend fun getActiveCategories(): List<Category>

    suspend fun getCategory(key: String): Category?

    suspend fun upsert(category: Category)

    suspend fun delete(key: String)

    suspend fun ensureDefaults()

    suspend fun countTransactionsUsing(key: String): Int
}
