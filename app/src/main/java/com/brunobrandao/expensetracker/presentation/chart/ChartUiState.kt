package com.brunobrandao.expensetracker.presentation.chart

import com.brunobrandao.expensetracker.data.preferences.Currency
import com.brunobrandao.expensetracker.domain.model.Category
import com.brunobrandao.expensetracker.presentation.home.TimePeriod

data class ChartUiState(
    val expensesByCategory: Map<String, Double> = emptyMap(),
    val categoriesMap: Map<String, Category> = emptyMap(),
    val totalExpenses: Double = 0.0,
    val totalIncome: Double = 0.0,
    val selectedPeriod: TimePeriod = TimePeriod.MONTHLY,
    val periodOffset: Int = 0,
    val periodLabel: String = "",
    val isLoading: Boolean = true,
    val currency: Currency = Currency.USD
)
