package com.brunobrandao.expensetracker.presentation.home

import com.brunobrandao.expensetracker.data.preferences.Currency
import com.brunobrandao.expensetracker.domain.model.Category
import com.brunobrandao.expensetracker.domain.model.Transaction

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
    val categoriesMap: Map<String, Category> = emptyMap(),
    val selectedPeriod: TimePeriod = TimePeriod.MONTHLY,
    val periodOffset: Int = 0,
    val periodLabel: String = "",
    val canGoNext: Boolean = false,
    val isLoading: Boolean = true,
    val currency: Currency = Currency.USD
)
