package com.brunobrandao.expensetracker.domain.usecase

import androidx.compose.ui.graphics.Color
import com.brunobrandao.expensetracker.domain.repository.CategoryRepository
import javax.inject.Inject

class UpdateCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(
        key: String,
        name: String,
        icon: String,
        color: Color,
        lightColor: Color
    ) {
        val existing = repository.getCategory(key)
            ?: throw NoSuchElementException("Category '$key' not found")

        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty()) { "Category name must not be empty" }

        // Update only the editable fields; key, isDefault, position, archived are preserved.
        repository.upsert(
            existing.copy(
                name = trimmedName,
                icon = icon,
                color = color,
                lightColor = lightColor
            )
        )
    }
}
