package com.brunobrandao.expensetracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val key: String,
    val name: String,
    val icon: String,
    val colorArgb: Int,
    val lightColorArgb: Int,
    val isDefault: Boolean,
    val position: Int,
    @ColumnInfo(defaultValue = "0") val archived: Boolean = false,
    val remoteId: String? = null,
    @ColumnInfo(defaultValue = "0") val synced: Boolean = false,
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0
)
