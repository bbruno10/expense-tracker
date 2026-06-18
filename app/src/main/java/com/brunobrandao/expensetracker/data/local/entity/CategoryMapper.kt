package com.brunobrandao.expensetracker.data.local.entity

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.brunobrandao.expensetracker.domain.model.Category

fun CategoryEntity.toDomain(): Category = Category(
    key = key,
    name = name,
    icon = icon,
    color = Color(colorArgb),
    lightColor = Color(lightColorArgb),
    isDefault = isDefault,
    position = position,
    archived = archived
)

fun Category.toEntity(
    remoteId: String? = null,
    synced: Boolean = false,
    updatedAt: Long = 0
): CategoryEntity = CategoryEntity(
    key = key,
    name = name,
    icon = icon,
    colorArgb = color.toArgb(),
    lightColorArgb = lightColor.toArgb(),
    isDefault = isDefault,
    position = position,
    archived = archived,
    remoteId = remoteId,
    synced = synced,
    updatedAt = updatedAt
)
