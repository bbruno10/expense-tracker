package com.example.expensetracker.presentation.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.domain.model.Transaction
import com.example.expensetracker.domain.repository.TransactionRepository
import com.example.expensetracker.domain.usecase.AddTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val addTransaction: AddTransactionUseCase,
    private val repository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    fun loadTransaction(transactionId: Long) {
        if (transactionId <= 0L) return
        viewModelScope.launch {
            repository.getTransactionById(transactionId)?.let { transaction ->
                _uiState.update {
                    it.copy(
                        editingId = transaction.id,
                        description = transaction.description,
                        amount = transaction.amount.toString(),
                        type = transaction.type,
                        category = transaction.category,
                        date = transaction.date,
                        note = transaction.note
                    )
                }
            }
        }
    }

    fun onEvent(event: AddTransactionEvent) {
        when (event) {
            is AddTransactionEvent.DescriptionChanged -> {
                _uiState.update { it.copy(description = event.value) }
            }
            is AddTransactionEvent.AmountChanged -> {
                val filtered = event.value.filter { char -> char.isDigit() || char == '.' }
                _uiState.update { it.copy(amount = filtered) }
            }
            is AddTransactionEvent.TypeChanged -> {
                _uiState.update { it.copy(type = event.value) }
            }
            is AddTransactionEvent.CategoryChanged -> {
                _uiState.update { it.copy(category = event.value) }
            }
            is AddTransactionEvent.DateChanged -> {
                _uiState.update { it.copy(date = event.value) }
            }
            is AddTransactionEvent.NoteChanged -> {
                _uiState.update { it.copy(note = event.value) }
            }
            is AddTransactionEvent.Save -> saveTransaction()
            is AddTransactionEvent.DismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    private fun saveTransaction() {
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
                val transaction = Transaction(
                    id = state.editingId ?: 0,
                    description = state.description.trim(),
                    amount = amount,
                    type = state.type,
                    category = state.category,
                    date = state.date,
                    note = state.note.trim()
                )
                if (state.isEditing) {
                    repository.updateTransaction(transaction)
                } else {
                    addTransaction(transaction)
                }
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to save: ${e.message}"
                    )
                }
            }
        }
    }
}
