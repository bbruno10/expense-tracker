package com.brunobrandao.expensetracker.domain.model

data class RecurringTransaction(
    val id: Long = 0,
    val description: String,
    val amount: Double,
    val type: TransactionType,
    val category: Category,
    val note: String = "",
    val frequency: RecurringFrequency,
    val startDate: Long,
    val nextDueDate: Long,
    val active: Boolean = true
)
