package com.example.expensetracker.presentation.settings

import com.example.expensetracker.data.preferences.Currency
import com.example.expensetracker.data.preferences.ThemeMode

data class SettingsUiState(
    val currency: Currency = Currency.USD,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appVersion: String = "1.0.0",
    val showCurrencyDialog: Boolean = false,
    val showThemeDialog: Boolean = false,
    val showExportSuccess: Boolean = false,
    val showAboutDialog: Boolean = false
)

sealed interface SettingsEvent {
    data class CurrencyChanged(val currency: Currency) : SettingsEvent
    data class ThemeModeChanged(val themeMode: ThemeMode) : SettingsEvent
    data object ToggleCurrencyDialog : SettingsEvent
    data object ToggleThemeDialog : SettingsEvent
    data object ToggleAboutDialog : SettingsEvent
    data object ExportData : SettingsEvent
    data object DismissExportSuccess : SettingsEvent
}
