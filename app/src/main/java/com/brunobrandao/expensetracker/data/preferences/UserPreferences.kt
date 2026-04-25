package com.brunobrandao.expensetracker.data.preferences

enum class Currency(val code: String, val symbol: String, val displayName: String) {
    USD("USD", "$", "US Dollar"),
    BRL("BRL", "R$", "Brazilian Real"),
    AUD("AUD", "A$", "Australian Dollar"),
    EUR("EUR", "€", "Euro"),
    GBP("GBP", "£", "British Pound"),
    JPY("JPY", "¥", "Japanese Yen")
}

enum class ThemeMode(val displayName: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark")
}

data class UserPreferences(
    val currency: Currency = Currency.USD,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
