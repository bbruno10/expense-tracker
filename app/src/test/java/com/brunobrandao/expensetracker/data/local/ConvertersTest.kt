package com.brunobrandao.expensetracker.data.local

import com.brunobrandao.expensetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ConvertersTest {

    private lateinit var converters: Converters

    @Before
    fun setup() {
        converters = Converters()
    }

    // ---- TransactionType ----

    @Test
    fun `fromTransactionType converts INCOME to string`() {
        val result = converters.fromTransactionType(TransactionType.INCOME)
        assertEquals("INCOME", result)
    }

    @Test
    fun `fromTransactionType converts EXPENSE to string`() {
        val result = converters.fromTransactionType(TransactionType.EXPENSE)
        assertEquals("EXPENSE", result)
    }

    @Test
    fun `toTransactionType converts string INCOME to enum`() {
        val result = converters.toTransactionType("INCOME")
        assertEquals(TransactionType.INCOME, result)
    }

    @Test
    fun `toTransactionType converts string EXPENSE to enum`() {
        val result = converters.toTransactionType("EXPENSE")
        assertEquals(TransactionType.EXPENSE, result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `toTransactionType throws exception for invalid value`() {
        converters.toTransactionType("INVALID")
    }
}
