package com.example.expensetracker.presentation.home

import com.example.expensetracker.domain.model.Transaction

enum class TimePeriod(val label: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

data class HomeUiState(
    val balance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val selectedPeriod: TimePeriod = TimePeriod.MONTHLY,
    val periodOffset: Int = 0, // 0 = current, -1 = previous, etc.
    val periodLabel: String = "",
    val canGoNext: Boolean = false,
    val isLoading: Boolean = true
)
