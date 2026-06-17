package com.brunobrandao.expensetracker.presentation.add

import com.brunobrandao.expensetracker.domain.model.Category
import com.brunobrandao.expensetracker.domain.model.RecurringFrequency
import com.brunobrandao.expensetracker.domain.model.TransactionType
import java.time.LocalDate
import java.time.ZoneOffset

data class AddTransactionUiState(
    val editingId: Long? = null,
    val description: String = "",
    val amount: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val category: String = "OTHER",
    val categories: List<Category> = emptyList(),
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val repeatEnabled: Boolean = false,
    val repeatFrequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val repeatStartDate: Long = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    val recurringId: Long? = null
) {
    val isEditing: Boolean get() = editingId != null
}

sealed interface AddTransactionEvent {
    data class DescriptionChanged(val value: String) : AddTransactionEvent
    data class AmountChanged(val value: String) : AddTransactionEvent
    data class TypeChanged(val value: TransactionType) : AddTransactionEvent
    data class CategoryChanged(val value: String) : AddTransactionEvent
    data class DateChanged(val value: Long) : AddTransactionEvent
    data class NoteChanged(val value: String) : AddTransactionEvent
    data object Save : AddTransactionEvent
    data object DismissError : AddTransactionEvent
    data object RepeatToggled : AddTransactionEvent
    data class FrequencyChanged(val value: RecurringFrequency) : AddTransactionEvent
    data class StartDateChanged(val value: Long) : AddTransactionEvent
}
