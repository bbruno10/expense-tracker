package com.brunobrandao.expensetracker.presentation.history

import com.brunobrandao.expensetracker.data.preferences.Currency
import com.brunobrandao.expensetracker.domain.model.Category
import com.brunobrandao.expensetracker.domain.model.Transaction
import com.brunobrandao.expensetracker.domain.model.TransactionType

enum class SearchScope { TITLE, NOTE, BOTH }

data class HistoryUiState(
    val transactions: List<Transaction> = emptyList(),
    val filterType: TransactionType? = null,
    val filterCategory: String? = null,
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val currency: Currency = Currency.USD,
    val searchQuery: String = "",
    val searchScope: SearchScope = SearchScope.BOTH
)

sealed interface HistoryEvent {
    data class FilterByType(val type: TransactionType?) : HistoryEvent
    data class FilterByCategory(val category: String?) : HistoryEvent
    data class DeleteTransaction(val id: Long) : HistoryEvent
    data class SearchQueryChanged(val query: String) : HistoryEvent
    data class SearchScopeChanged(val scope: SearchScope) : HistoryEvent
    data object ClearSearch : HistoryEvent
}
