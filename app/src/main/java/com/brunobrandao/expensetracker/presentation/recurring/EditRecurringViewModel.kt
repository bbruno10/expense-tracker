package com.brunobrandao.expensetracker.presentation.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brunobrandao.expensetracker.data.sync.SyncRepository
import com.brunobrandao.expensetracker.domain.model.Category
import com.brunobrandao.expensetracker.domain.model.RecurringFrequency
import com.brunobrandao.expensetracker.domain.model.RecurringTransaction
import com.brunobrandao.expensetracker.domain.model.TransactionType
import com.brunobrandao.expensetracker.domain.repository.AuthRepository
import com.brunobrandao.expensetracker.domain.repository.CategoryRepository
import com.brunobrandao.expensetracker.domain.repository.RecurringTransactionRepository
import com.brunobrandao.expensetracker.domain.util.Clock
import com.brunobrandao.expensetracker.domain.util.RecurringDateCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditRecurringUiState(
    val id: Long = 0L,
    val description: String = "",
    val amount: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val category: String = "OTHER",
    val categories: List<Category> = emptyList(),
    val note: String = "",
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val active: Boolean = true,
    val startDate: Long = 0L,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null,
    val notFound: Boolean = false
)

sealed interface EditRecurringEvent {
    data class DescriptionChanged(val value: String) : EditRecurringEvent
    data class AmountChanged(val value: String) : EditRecurringEvent
    data class TypeChanged(val value: TransactionType) : EditRecurringEvent
    data class CategoryChanged(val value: String) : EditRecurringEvent
    data class NoteChanged(val value: String) : EditRecurringEvent
    data class FrequencyChanged(val value: RecurringFrequency) : EditRecurringEvent
    data class ActiveChanged(val value: Boolean) : EditRecurringEvent
    data class StartDateChanged(val value: Long) : EditRecurringEvent
    data object Save : EditRecurringEvent
    data object Delete : EditRecurringEvent
    data object DismissError : EditRecurringEvent
}

@HiltViewModel
class EditRecurringViewModel @Inject constructor(
    private val recurringRepository: RecurringTransactionRepository,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository,
    private val categoryRepository: CategoryRepository,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditRecurringUiState())
    val uiState: StateFlow<EditRecurringUiState> = _uiState.asStateFlow()

    init {
        categoryRepository.observeCategories().onEach { cats ->
            _uiState.update { it.copy(categories = cats) }
        }.launchIn(viewModelScope)
    }

    fun load(recurringId: Long) {
        if (recurringId <= 0L) {
            _uiState.update { it.copy(notFound = true) }
            return
        }
        viewModelScope.launch {
            val rule = recurringRepository.getById(recurringId)
            if (rule == null) {
                _uiState.update { it.copy(notFound = true) }
            } else {
                _uiState.update {
                    it.copy(
                        id = rule.id,
                        description = rule.description,
                        amount = rule.amount.toString(),
                        type = rule.type,
                        category = rule.category,
                        note = rule.note,
                        frequency = rule.frequency,
                        active = rule.active,
                        startDate = rule.startDate
                    )
                }
            }
        }
    }

    fun onEvent(event: EditRecurringEvent) {
        when (event) {
            is EditRecurringEvent.DescriptionChanged ->
                _uiState.update { it.copy(description = event.value) }
            is EditRecurringEvent.AmountChanged -> {
                val filtered = event.value.filter { c -> c.isDigit() || c == '.' }
                _uiState.update { it.copy(amount = filtered) }
            }
            is EditRecurringEvent.TypeChanged ->
                _uiState.update { it.copy(type = event.value) }
            is EditRecurringEvent.CategoryChanged ->
                _uiState.update { it.copy(category = event.value) }
            is EditRecurringEvent.NoteChanged ->
                _uiState.update { it.copy(note = event.value) }
            is EditRecurringEvent.FrequencyChanged ->
                _uiState.update { it.copy(frequency = event.value) }
            is EditRecurringEvent.ActiveChanged ->
                _uiState.update { it.copy(active = event.value) }
            is EditRecurringEvent.StartDateChanged ->
                _uiState.update { it.copy(startDate = event.value) }
            is EditRecurringEvent.Save -> save()
            is EditRecurringEvent.Delete -> delete()
            is EditRecurringEvent.DismissError ->
                _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun save() {
        val state = _uiState.value
        if (state.description.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a description") }
            return
        }
        val amount = state.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid amount") }
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                recurringRepository.upsert(
                    RecurringTransaction(
                        id = state.id,
                        description = state.description.trim(),
                        amount = amount,
                        type = state.type,
                        category = state.category,
                        note = state.note.trim(),
                        frequency = state.frequency,
                        startDate = state.startDate,
                        nextDueDate = RecurringDateCalculator.computeNextDueDate(
                            startDate = state.startDate,
                            frequency = state.frequency,
                            now = clock.now()
                        ),
                        active = state.active
                    )
                )
                authRepository.currentUserId?.let { userId ->
                    try { syncRepository.syncWriteRecurring(state.id, userId) } catch (_: Exception) {}
                }
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to save: ${e.message}") }
            }
        }
    }

    private fun delete() {
        val state = _uiState.value
        viewModelScope.launch {
            try {
                val userId = authRepository.currentUserId
                if (userId != null) {
                    syncRepository.syncDeleteRecurring(state.id, userId)
                } else {
                    recurringRepository.deleteById(state.id)
                }
                _uiState.update { it.copy(isDeleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete: ${e.message}") }
            }
        }
    }
}
