package com.brunobrandao.expensetracker.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class FinanceColors(
    val income: Color,
    val expense: Color
)

private val LightFinanceColors = FinanceColors(
    income = IncomeGreen,
    expense = ExpenseRed
)

private val DarkFinanceColors = FinanceColors(
    income = Color(0xFF66BB6A),
    expense = Color(0xFFEF5350)
)

val LocalFinanceColors = staticCompositionLocalOf { LightFinanceColors }

internal fun financeColorsForTheme(isDark: Boolean): FinanceColors =
    if (isDark) DarkFinanceColors else LightFinanceColors
