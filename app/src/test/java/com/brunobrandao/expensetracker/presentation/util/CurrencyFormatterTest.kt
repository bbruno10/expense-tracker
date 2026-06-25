package com.brunobrandao.expensetracker.presentation.util

import com.brunobrandao.expensetracker.data.preferences.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun `format USD uses dollar sign and US locale`() {
        val result = CurrencyFormatter.format(1234.56, Currency.USD)
        assertTrue("Expected dollar sign, got: $result", result.contains("$") || result.contains("USD"))
        assertTrue("Expected 1,234.56, got: $result", result.contains("1,234.56"))
    }

    @Test
    fun `format BRL uses R symbol`() {
        val result = CurrencyFormatter.format(1000.00, Currency.BRL)
        assertTrue("Expected BRL symbol, got: $result", result.contains("R$") || result.contains("BRL"))
    }

    @Test
    fun `format EUR uses euro symbol`() {
        val result = CurrencyFormatter.format(500.00, Currency.EUR)
        assertTrue("Expected euro symbol, got: $result", result.contains("€") || result.contains("EUR"))
    }

    @Test
    fun `format GBP uses pound symbol`() {
        val result = CurrencyFormatter.format(100.00, Currency.GBP)
        assertTrue("Expected pound symbol, got: $result", result.contains("£") || result.contains("GBP"))
    }

    @Test
    fun `format JPY uses yen symbol`() {
        val result = CurrencyFormatter.format(1000.00, Currency.JPY)
        // JVM may produce ¥ (U+00A5) or ￥ (U+FFE5) depending on locale data
        assertTrue("Expected yen symbol, got: $result", result.contains("¥") || result.contains("￥") || result.contains("JPY"))
    }

    @Test
    fun `formatCompact returns K suffix for amounts above 10k`() {
        val result = CurrencyFormatter.formatCompact(15000.0, Currency.USD)
        assertTrue("Expected K suffix, got: $result", result.contains("K"))
        assertTrue("Expected 15.0K, got: $result", result.contains("15.0"))
    }

    @Test
    fun `formatCompact returns M suffix for amounts above 1M`() {
        val result = CurrencyFormatter.formatCompact(2_500_000.0, Currency.USD)
        assertTrue("Expected M suffix, got: $result", result.contains("M"))
        assertTrue("Expected 2.5M, got: $result", result.contains("2.5"))
    }

    @Test
    fun `formatCompact delegates to format for small amounts`() {
        val compact = CurrencyFormatter.formatCompact(99.99, Currency.USD)
        val full = CurrencyFormatter.format(99.99, Currency.USD)
        assertEquals(full, compact)
    }

    @Test
    fun `format result differs by currency for same amount`() {
        val usd = CurrencyFormatter.format(100.0, Currency.USD)
        val eur = CurrencyFormatter.format(100.0, Currency.EUR)
        assertTrue("USD and EUR formatting should differ", usd != eur)
    }

    @Test
    fun `format is stateless - repeated calls with different currencies produce correct results`() {
        val usd1 = CurrencyFormatter.format(50.0, Currency.USD)
        CurrencyFormatter.format(50.0, Currency.EUR)
        val usd2 = CurrencyFormatter.format(50.0, Currency.USD)
        assertEquals("Repeated USD calls should produce identical results", usd1, usd2)
    }
}
