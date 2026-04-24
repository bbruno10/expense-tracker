package com.example.expensetracker.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.domain.model.TransactionType
import com.example.expensetracker.domain.usecase.DeleteTransactionUseCase
import com.example.expensetracker.domain.usecase.GetBalanceUseCase
import com.example.expensetracker.domain.usecase.GetTotalByTypeUseCase
import com.example.expensetracker.domain.usecase.GetTransactionsByDateRangeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTransactionsByDateRange: GetTransactionsByDateRangeUseCase,
    private val getBalance: GetBalanceUseCase,
    private val getTotalByType: GetTotalByTypeUseCase,
    private val deleteTransaction: DeleteTransactionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

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

    fun onDeleteTransaction(id: Long) {
        viewModelScope.launch {
            deleteTransaction(id)
        }
    }

    private fun observeData() {
        combine(_selectedPeriod, _periodOffset, _refreshTrigger) { period, offset, _ ->
            Pair(period, offset)
        }.flatMapLatest { (period, offset) ->
            val (start, end) = getDateRange(period, offset)
            val label = getPeriodLabel(period, offset)
            combine(
                getBalance.byDateRange(start, end),
                getTotalByType.byDateRange(TransactionType.INCOME, start, end),
                getTotalByType.byDateRange(TransactionType.EXPENSE, start, end),
                getTransactionsByDateRange(start, end)
            ) { balance, income, expense, transactions ->
                HomeUiState(
                    balance = balance,
                    totalIncome = income,
                    totalExpense = expense,
                    recentTransactions = transactions.take(10),
                    selectedPeriod = period,
                    periodOffset = offset,
                    periodLabel = label,
                    canGoNext = offset < 0,
                    isLoading = false
                )
            }
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    companion object {
        private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)

        fun getDateRange(period: TimePeriod, offset: Int): Pair<Long, Long> {
            val calendar = Calendar.getInstance()

            when (period) {
                TimePeriod.WEEKLY -> {
                    calendar.add(Calendar.WEEK_OF_YEAR, offset)
                    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val start = calendar.timeInMillis
                    calendar.add(Calendar.DAY_OF_YEAR, 7)
                    val end = calendar.timeInMillis - 1
                    return Pair(start, end)
                }
                TimePeriod.MONTHLY -> {
                    calendar.add(Calendar.MONTH, offset)
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val start = calendar.timeInMillis
                    calendar.add(Calendar.MONTH, 1)
                    val end = calendar.timeInMillis - 1
                    return Pair(start, end)
                }
                TimePeriod.YEARLY -> {
                    calendar.add(Calendar.YEAR, offset)
                    calendar.set(Calendar.DAY_OF_YEAR, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val start = calendar.timeInMillis
                    calendar.add(Calendar.YEAR, 1)
                    val end = calendar.timeInMillis - 1
                    return Pair(start, end)
                }
            }
        }

        fun getPeriodLabel(period: TimePeriod, offset: Int): String {
            val calendar = Calendar.getInstance()
            return when (period) {
                TimePeriod.WEEKLY -> {
                    calendar.add(Calendar.WEEK_OF_YEAR, offset)
                    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    val startFormat = SimpleDateFormat("MMM dd", Locale.US)
                    val start = startFormat.format(calendar.time)
                    calendar.add(Calendar.DAY_OF_YEAR, 6)
                    val end = startFormat.format(calendar.time)
                    "$start - $end"
                }
                TimePeriod.MONTHLY -> {
                    calendar.add(Calendar.MONTH, offset)
                    monthFormat.format(calendar.time).replaceFirstChar { it.uppercase() }
                }
                TimePeriod.YEARLY -> {
                    calendar.add(Calendar.YEAR, offset)
                    calendar.get(Calendar.YEAR).toString()
                }
            }
        }
    }
}
