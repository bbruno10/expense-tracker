package com.example.expensetracker.data.local.entity

import com.example.expensetracker.domain.model.Transaction

fun TransactionEntity.toDomain(): Transaction {
    return Transaction(
        id = id,
        description = description,
        amount = amount,
        type = type,
        category = category,
        date = date,
        note = note,
        createdAt = createdAt
    )
}

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        description = description,
        amount = amount,
        type = type,
        category = category,
        date = date,
        note = note,
        createdAt = createdAt
    )
}
