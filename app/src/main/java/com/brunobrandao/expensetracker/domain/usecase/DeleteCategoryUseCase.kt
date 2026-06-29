package com.brunobrandao.expensetracker.domain.usecase

import com.brunobrandao.expensetracker.domain.repository.CategoryRepository
import javax.inject.Inject

/**
 * Validates that a category is eligible for deletion (not default, not in use).
 * The actual removal — local + Firestore — is the caller's responsibility:
 * use SyncRepository.syncDeleteCategory when online, CategoryRepository.delete when offline.
 */
class DeleteCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(key: String) {
        val category = repository.getCategory(key)
            ?: throw NoSuchElementException("Category '$key' not found")

        check(!category.isDefault) {
            "Default category '$key' cannot be deleted"
        }

        val count = repository.countTransactionsUsing(key)
        check(count == 0) {
            "Category '$key' is used by $count transaction(s); archive it instead of deleting"
        }
    }
}
