package com.brunobrandao.expensetracker.presentation.history

import com.brunobrandao.expensetracker.domain.model.Category
import com.brunobrandao.expensetracker.domain.model.Transaction
import com.brunobrandao.expensetracker.domain.model.TransactionType

data class HistoryUiState(
    val transactions: List<Transaction> = emptyList(),
    val filterType: TransactionType? = null, // null = todos
    val filterCategory: Category? = null, // null = todas
    val isLoading: Boolean = true
)

sealed interface HistoryEvent {
    data class FilterByType(val type: TransactionType?) : HistoryEvent
    data class FilterByCategory(val category: Category?) : HistoryEvent
    data class DeleteTransaction(val id: Long) : HistoryEvent
}
