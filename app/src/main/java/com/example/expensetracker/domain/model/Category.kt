package com.example.expensetracker.domain.model

import androidx.compose.ui.graphics.Color

enum class Category(
    val displayName: String,
    val icon: String,
    val color: Color,
    val lightColor: Color
) {
    FOOD("Food", "🍔", Color(0xFFD85A30), Color(0xFFFAECE7)),
    TRANSPORT("Transport", "🚗", Color(0xFF378ADD), Color(0xFFE6F1FB)),
    HOUSING("Housing", "🏠", Color(0xFF2A9D8F), Color(0xFFCCEFEB)),
    HEALTH("Health", "💊", Color(0xFFE24B4A), Color(0xFFFCEBEB)),
    EDUCATION("Education", "📚", Color(0xFF5C6BC0), Color(0xFFE8EAF6)),
    ENTERTAINMENT("Entertainment", "🎬", Color(0xFF7F77DD), Color(0xFFEEEDFE)),
    SHOPPING("Shopping", "🛒", Color(0xFFD4537E), Color(0xFFFBEAF0)),
    BILLS("Bills", "📄", Color(0xFFBA7517), Color(0xFFFAEEDA)),
    SALARY("Salary", "💰", Color(0xFF1D9E75), Color(0xFFE1F5EE)),
    INVESTMENT("Investment", "📈", Color(0xFF5DCAA5), Color(0xFFDFF5ED)),
    OTHER("Other", "📌", Color(0xFF78909C), Color(0xFFECEFF1))
}
