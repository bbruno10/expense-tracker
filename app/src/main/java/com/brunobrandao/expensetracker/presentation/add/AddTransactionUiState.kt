package com.brunobrandao.expensetracker.presentation.add

import com.brunobrandao.expensetracker.domain.model.Category
import com.brunobrandao.expensetracker.domain.model.TransactionType

data class AddTransactionUiState(
    val editingId: Long? = null,
    val description: String = "",
    val amount: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val category: Category = Category.OTHER,
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
) {
    val isEditing: Boolean get() = editingId != null
}

sealed interface AddTransactionEvent {
    data class DescriptionChanged(val value: String) : AddTransactionEvent
    data class AmountChanged(val value: String) : AddTransactionEvent
    data class TypeChanged(val value: TransactionType) : AddTransactionEvent
    data class CategoryChanged(val value: Category) : AddTransactionEvent
    data class DateChanged(val value: Long) : AddTransactionEvent
    data class NoteChanged(val value: String) : AddTransactionEvent
    data object Save : AddTransactionEvent
    data object DismissError : AddTransactionEvent
}
