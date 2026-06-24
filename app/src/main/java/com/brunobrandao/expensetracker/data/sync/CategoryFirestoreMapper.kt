package com.brunobrandao.expensetracker.data.sync

import com.brunobrandao.expensetracker.data.local.entity.CategoryEntity
import com.google.firebase.firestore.DocumentSnapshot

fun CategoryEntity.toFirestoreMap(): Map<String, Any> = buildMap {
    put("key", key)
    put("name", name)
    put("icon", icon)
    put("colorArgb", colorArgb)
    put("lightColorArgb", lightColorArgb)
    put("isDefault", isDefault)
    put("position", position)
    put("archived", archived)
    put("updatedAt", updatedAt)
}

fun DocumentSnapshot.toCategoryEntity(): CategoryEntity? {
    return try {
        CategoryEntity(
            key = getString("key") ?: return null,
            name = getString("name") ?: return null,
            icon = getString("icon") ?: return null,
            colorArgb = getLong("colorArgb")?.toInt() ?: return null,
            lightColorArgb = getLong("lightColorArgb")?.toInt() ?: return null,
            isDefault = getBoolean("isDefault") ?: false,
            position = getLong("position")?.toInt() ?: 0,
            archived = getBoolean("archived") ?: false,
            remoteId = id,
            updatedAt = getLong("updatedAt") ?: System.currentTimeMillis(),
            synced = true
        )
    } catch (_: Exception) {
        null
    }
}
