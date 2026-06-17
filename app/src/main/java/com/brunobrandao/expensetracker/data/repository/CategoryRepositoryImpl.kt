package com.brunobrandao.expensetracker.data.repository

import com.brunobrandao.expensetracker.data.local.dao.CategoryDao
import com.brunobrandao.expensetracker.data.local.entity.toDomain
import com.brunobrandao.expensetracker.data.local.entity.toEntity
import com.brunobrandao.expensetracker.domain.model.Category
import com.brunobrandao.expensetracker.domain.model.DefaultCategories
import com.brunobrandao.expensetracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getCategory(key: String): Category? =
        dao.getByKey(key)?.toDomain()

    override suspend fun upsert(category: Category) {
        val existing = dao.getByKey(category.key)
        dao.upsert(
            category.toEntity(
                remoteId = existing?.remoteId,
                synced = false,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun delete(key: String) {
        dao.deleteByKey(key)
    }

    override suspend fun ensureDefaults() {
        dao.insertIgnore(DefaultCategories.LIST.map { it.toEntity() })
    }

    override suspend fun countTransactionsUsing(key: String): Int =
        dao.countTransactionsUsing(key)
}
