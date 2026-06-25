package com.brunobrandao.expensetracker.presentation.util

import com.brunobrandao.expensetracker.data.preferences.Currency
import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    fun format(amount: Double, currency: Currency): String {
        val locale = when (currency) {
            Currency.USD -> Locale.US
            Currency.BRL -> Locale("pt", "BR")
            Currency.AUD -> Locale("en", "AU")
            Currency.EUR -> Locale.GERMANY
            Currency.GBP -> Locale.UK
            Currency.JPY -> Locale.JAPAN
        }
        return NumberFormat.getCurrencyInstance(locale).format(amount)
    }

    fun formatCompact(amount: Double, currency: Currency): String {
        val symbol = currency.symbol
        return when {
            amount >= 1_000_000 -> String.format(Locale.US, "%s%.1fM", symbol, amount / 1_000_000)
            amount >= 10_000 -> String.format(Locale.US, "%s%.1fK", symbol, amount / 1_000)
            else -> format(amount, currency)
        }
    }
}
