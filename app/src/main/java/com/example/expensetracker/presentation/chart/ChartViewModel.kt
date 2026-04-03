package com.example.expensetracker.presentation.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.domain.model.TransactionType
import com.example.expensetracker.domain.usecase.GetTotalByTypeUseCase
import com.example.expensetracker.domain.usecase.GetTransactionsByDateRangeUseCase
import com.example.expensetracker.presentation.home.HomeViewModel
import com.example.expensetracker.presentation.home.TimePeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class ChartViewModel @Inject constructor(
    private val getTransactionsByDateRange: GetTransactionsByDateRangeUseCase,
    private val getTotalByType: GetTotalByTypeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChartUiState())
    val uiState: StateFlow<ChartUiState> = _uiState.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(TimePeriod.MONTHLY)
    private val _periodOffset = MutableStateFlow(0)
    private val _refreshTrigger = MutableStateFlow(0L)

    init {
        observeData()
    }

    fun refresh() {
        _refreshTrigger.value = System.currentTimeMillis()
    }

    fun onPeriodChanged(period: TimePeriod) {
        _periodOffset.value = 0
        _selectedPeriod.value = period
    }

    fun onPreviousPeriod() {
        _periodOffset.value -= 1
    }

    fun onNextPeriod() {
        if (_periodOffset.value < 0) {
            _periodOffset.value += 1
        }
    }

    private fun observeData() {
        combine(_selectedPeriod, _periodOffset, _refreshTrigger) { period, offset, _ ->
            Pair(period, offset)
        }.flatMapLatest { (period, offset) ->
            val (start, end) = HomeViewModel.getDateRange(period, offset)
            val label = HomeViewModel.getPeriodLabel(period, offset)
            combine(
                getTransactionsByDateRange(start, end),
                getTotalByType.byDateRange(TransactionType.EXPENSE, start, end),
                getTotalByType.byDateRange(TransactionType.INCOME, start, end)
            ) { transactions, totalExpenses, totalIncome ->

                val expensesByCategory = transactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .groupBy { it.category }
                    .mapValues { (_, txs) -> txs.sumOf { it.amount } }
                    .entries
                    .sortedByDescending { it.value }
                    .associate { it.key to it.value }

                ChartUiState(
                    expensesByCategory = expensesByCategory,
                    totalExpenses = totalExpenses,
                    totalIncome = totalIncome,
                    selectedPeriod = period,
                    periodOffset = offset,
                    periodLabel = label,
                    isLoading = false
                )
            }
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }
}
