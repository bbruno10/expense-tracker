package com.brunobrandao.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.brunobrandao.expensetracker.domain.model.RecurringFrequency
import com.brunobrandao.expensetracker.domain.model.TransactionType

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val note: String = "",
    val frequency: RecurringFrequency,
    val startDate: Long,
    val nextDueDate: Long,
    val active: Boolean = true,
    val remoteId: String = "",
    val synced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
