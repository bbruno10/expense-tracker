package com.brunobrandao.expensetracker.domain.usecase

import com.brunobrandao.expensetracker.domain.repository.CategoryRepository
import javax.inject.Inject

class ArchiveCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(key: String) {
        val existing = repository.getCategory(key)
            ?: throw NoSuchElementException("Category '$key' not found")
        repository.upsert(existing.copy(archived = true))
    }
}
