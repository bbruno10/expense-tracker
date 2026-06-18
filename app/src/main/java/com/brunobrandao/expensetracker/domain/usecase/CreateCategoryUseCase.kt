package com.brunobrandao.expensetracker.domain.usecase

import androidx.compose.ui.graphics.Color
import com.brunobrandao.expensetracker.domain.model.Category
import com.brunobrandao.expensetracker.domain.repository.CategoryRepository
import java.util.UUID
import javax.inject.Inject

class CreateCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(
        name: String,
        icon: String,
        color: Color,
        lightColor: Color
    ): Category {
        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty()) { "Category name must not be empty" }

        val activeCategories = repository.getActiveCategories()
        val duplicate = activeCategories.any { it.name.trim().equals(trimmedName, ignoreCase = true) }
        require(!duplicate) { "A category named '$trimmedName' already exists" }

        val category = Category(
            key = UUID.randomUUID().toString(),
            name = trimmedName,
            icon = icon,
            color = color,
            lightColor = lightColor,
            isDefault = false,
            position = Int.MAX_VALUE,
            archived = false
        )
        repository.upsert(category)
        return category
    }
}
