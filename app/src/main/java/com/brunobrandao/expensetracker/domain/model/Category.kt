package com.brunobrandao.expensetracker.domain.model

import androidx.compose.ui.graphics.Color

data class Category(
    val key: String,
    val name: String,
    val icon: String,
    val color: Color,
    val lightColor: Color,
    val isDefault: Boolean = false,
    val position: Int = 0,
    val archived: Boolean = false
)
