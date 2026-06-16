package com.brunobrandao.expensetracker.data.local.entity

import com.brunobrandao.expensetracker.domain.model.Transaction

fun TransactionEntity.toDomain(): Transaction {
    return Transaction(
        id = id,
        description = description,
        amount = amount,
        type = type,
        category = category,
        date = date,
        note = note,
        createdAt = createdAt,
        recurringId = recurringId
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
        createdAt = createdAt,
        recurringId = recurringId
    )
}
