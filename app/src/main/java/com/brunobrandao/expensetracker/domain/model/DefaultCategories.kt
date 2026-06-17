package com.brunobrandao.expensetracker.domain.model

import androidx.compose.ui.graphics.Color

object DefaultCategories {

    val LIST: List<Category> = listOf(
        Category("FOOD",          "Food",          "🍔", Color(0xFFD85A30), Color(0xFFFAECE7), isDefault = true, position = 0),
        Category("TRANSPORT",     "Transport",     "🚗", Color(0xFF378ADD), Color(0xFFE6F1FB), isDefault = true, position = 1),
        Category("HOUSING",       "Housing",       "🏠", Color(0xFF2A9D8F), Color(0xFFCCEFEB), isDefault = true, position = 2),
        Category("HEALTH",        "Health",        "💊", Color(0xFFE24B4A), Color(0xFFFCEBEB), isDefault = true, position = 3),
        Category("EDUCATION",     "Education",     "📚", Color(0xFF5C6BC0), Color(0xFFE8EAF6), isDefault = true, position = 4),
        Category("ENTERTAINMENT", "Entertainment", "🎬", Color(0xFF7F77DD), Color(0xFFEEEDFE), isDefault = true, position = 5),
        Category("SHOPPING",      "Shopping",      "🛒", Color(0xFFD4537E), Color(0xFFFBEAF0), isDefault = true, position = 6),
        Category("BILLS",         "Bills",         "📄", Color(0xFFBA7517), Color(0xFFFAEEDA), isDefault = true, position = 7),
        Category("SALARY",        "Salary",        "💰", Color(0xFF1D9E75), Color(0xFFE1F5EE), isDefault = true, position = 8),
        Category("INVESTMENT",    "Investment",    "📈", Color(0xFF5DCAA5), Color(0xFFDFF5ED), isDefault = true, position = 9),
        Category("OTHER",         "Other",         "📌", Color(0xFF78909C), Color(0xFFECEFF1), isDefault = true, position = 10)
    )

    fun fallback(key: String): Category = Category(
        key = key,
        name = key,
        icon = "📌",
        color = Color(0xFF78909C),
        lightColor = Color(0xFFECEFF1),
        isDefault = false,
        position = Int.MAX_VALUE
    )
}
