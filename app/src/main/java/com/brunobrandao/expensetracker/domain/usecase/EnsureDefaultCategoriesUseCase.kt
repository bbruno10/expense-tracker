package com.brunobrandao.expensetracker.domain.usecase

import com.brunobrandao.expensetracker.domain.repository.CategoryRepository
import javax.inject.Inject

class EnsureDefaultCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke() {
        categoryRepository.ensureDefaults()
    }
}
