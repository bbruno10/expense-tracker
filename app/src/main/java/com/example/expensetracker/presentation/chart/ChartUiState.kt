package com.example.expensetracker.presentation.chart

import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.presentation.home.TimePeriod

data class ChartUiState(
    val expensesByCategory: Map<Category, Double> = emptyMap(),
    val totalExpenses: Double = 0.0,
    val totalIncome: Double = 0.0,
    val selectedPeriod: TimePeriod = TimePeriod.MONTHLY,
    val periodOffset: Int = 0,
    val periodLabel: String = "",
    val isLoading: Boolean = true
)
