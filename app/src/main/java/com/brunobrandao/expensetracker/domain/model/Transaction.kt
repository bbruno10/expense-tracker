package com.brunobrandao.expensetracker.domain.model

data class Transaction(
    val id: Long = 0,
    val description: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val date: Long,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val recurringId: Long? = null
)
