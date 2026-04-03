package com.example.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val amount: Double,
    val type: TransactionType,
    val category: Category,
    val date: Long, // timestamp em millis
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
