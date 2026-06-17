package com.brunobrandao.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.brunobrandao.expensetracker.domain.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val date: Long,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val remoteId: String = "",
    val synced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val recurringId: Long? = null
)
