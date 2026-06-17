package com.brunobrandao.expensetracker.presentation.history

import com.brunobrandao.expensetracker.domain.model.Category
import com.brunobrandao.expensetracker.domain.model.Transaction
import com.brunobrandao.expensetracker.domain.model.TransactionType

data class HistoryUiState(
    val transactions: List<Transaction> = emptyList(),
    val filterType: TransactionType? = null,
    val filterCategory: String? = null,
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface HistoryEvent {
    data class FilterByType(val type: TransactionType?) : HistoryEvent
    data class FilterByCategory(val category: String?) : HistoryEvent
    data class DeleteTransaction(val id: Long) : HistoryEvent
}
