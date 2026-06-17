package com.brunobrandao.expensetracker.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brunobrandao.expensetracker.domain.repository.CategoryRepository
import com.brunobrandao.expensetracker.domain.usecase.DeleteTransactionUseCase
import com.brunobrandao.expensetracker.domain.usecase.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getTransactions: GetTransactionsUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        observeTransactions()
        categoryRepository.observeCategories().onEach { cats ->
            _uiState.update { it.copy(categories = cats) }
        }.launchIn(viewModelScope)
    }

    private fun observeTransactions() {
        getTransactions().onEach { allTransactions ->
            val state = _uiState.value
            val filtered = allTransactions
                .filter { transaction ->
                    (state.filterType == null || transaction.type == state.filterType) &&
                    (state.filterCategory == null || transaction.category == state.filterCategory)
                }
            _uiState.update {
                it.copy(transactions = filtered, isLoading = false)
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: HistoryEvent) {
        when (event) {
            is HistoryEvent.FilterByType -> {
                _uiState.update { it.copy(filterType = event.type) }
                observeTransactions()
            }
            is HistoryEvent.FilterByCategory -> {
                _uiState.update { it.copy(filterCategory = event.category) }
                observeTransactions()
            }
            is HistoryEvent.DeleteTransaction -> {
                viewModelScope.launch {
                    deleteTransaction(event.id)
                }
            }
        }
    }
}
