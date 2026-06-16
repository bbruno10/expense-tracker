package com.brunobrandao.expensetracker.data.local.entity

import com.brunobrandao.expensetracker.domain.model.RecurringTransaction

fun RecurringTransactionEntity.toDomain(): RecurringTransaction {
    return RecurringTransaction(
        id = id,
        description = description,
        amount = amount,
        type = type,
        category = category,
        note = note,
        frequency = frequency,
        startDate = startDate,
        nextDueDate = nextDueDate,
        active = active
    )
}

fun RecurringTransaction.toEntity(): RecurringTransactionEntity {
    return RecurringTransactionEntity(
        id = id,
        description = description,
        amount = amount,
        type = type,
        category = category,
        note = note,
        frequency = frequency,
        startDate = startDate,
        nextDueDate = nextDueDate,
        active = active
    )
}
