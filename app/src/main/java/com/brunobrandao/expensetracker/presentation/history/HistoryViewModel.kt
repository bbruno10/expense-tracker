package com.brunobrandao.expensetracker.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brunobrandao.expensetracker.data.preferences.UserPreferencesRepository
import com.brunobrandao.expensetracker.data.sync.SyncRepository
import com.brunobrandao.expensetracker.domain.model.Transaction
import com.brunobrandao.expensetracker.domain.repository.AuthRepository
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
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var allTransactions: List<Transaction> = emptyList()

    init {
        getTransactions().onEach { list ->
            allTransactions = list
            applyFilters()
        }.launchIn(viewModelScope)
        categoryRepository.observeCategories().onEach { cats ->
            _uiState.update { it.copy(categories = cats) }
        }.launchIn(viewModelScope)
        preferencesRepository.userPreferences
            .onEach { prefs -> _uiState.update { it.copy(currency = prefs.currency) } }
            .launchIn(viewModelScope)
    }

    private fun applyFilters() {
        val state = _uiState.value
        val filtered = allTransactions.filter { transaction ->
            (state.filterType == null || transaction.type == state.filterType) &&
            (state.filterCategory == null || transaction.category == state.filterCategory) &&
            TransactionSearchMatcher.matches(transaction, state.searchQuery, state.searchScope)
        }
        _uiState.update { it.copy(transactions = filtered, isLoading = false) }
    }

    fun onEvent(event: HistoryEvent) {
        when (event) {
            is HistoryEvent.FilterByType -> {
                _uiState.update { it.copy(filterType = event.type) }
                applyFilters()
            }
            is HistoryEvent.FilterByCategory -> {
                _uiState.update { it.copy(filterCategory = event.category) }
                applyFilters()
            }
            is HistoryEvent.SearchQueryChanged -> {
                _uiState.update { it.copy(searchQuery = event.query) }
                applyFilters()
            }
            is HistoryEvent.SearchScopeChanged -> {
                _uiState.update { it.copy(searchScope = event.scope) }
                applyFilters()
            }
            is HistoryEvent.ClearSearch -> {
                _uiState.update { it.copy(searchQuery = "") }
                applyFilters()
            }
            is HistoryEvent.DeleteTransaction -> {
                viewModelScope.launch {
                    val userId = authRepository.currentUserId
                    if (userId != null) {
                        syncRepository.syncDelete(event.id, userId)
                    } else {
                        deleteTransaction(event.id)
                    }
                }
            }
        }
    }
}
