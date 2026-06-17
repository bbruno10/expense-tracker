package com.brunobrandao.expensetracker.data.repository

import com.brunobrandao.expensetracker.data.local.dao.TransactionDao
import com.brunobrandao.expensetracker.data.local.entity.TransactionEntity
import com.brunobrandao.expensetracker.domain.model.Transaction
import com.brunobrandao.expensetracker.domain.model.TransactionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TransactionRepositoryImplTest {

    private lateinit var dao: TransactionDao
    private lateinit var repository: TransactionRepositoryImpl

    private val sampleEntity = TransactionEntity(
        id = 1L,
        description = "Lunch",
        amount = 25.50,
        type = TransactionType.EXPENSE,
        category = "FOOD",
        date = 1700000000000L,
        note = "Downtown restaurant",
        createdAt = 1700000000000L
    )

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        repository = TransactionRepositoryImpl(dao)
    }

    // ---- getAllTransactions ----

    @Test
    fun `getAllTransactions maps entities to domain models`() = runTest {
        every { dao.getAllTransactions() } returns flowOf(listOf(sampleEntity))

        val result = repository.getAllTransactions().first()

        assertEquals(1, result.size)
        assertEquals("Lunch", result[0].description)
        assertEquals(25.50, result[0].amount, 0.001)
        assertEquals(TransactionType.EXPENSE, result[0].type)
        assertEquals("FOOD", result[0].category)
    }

    @Test
    fun `getAllTransactions returns empty list when no data`() = runTest {
        every { dao.getAllTransactions() } returns flowOf(emptyList())

        val result = repository.getAllTransactions().first()

        assertEquals(0, result.size)
    }

    // ---- getTransactionById ----

    @Test
    fun `getTransactionById returns mapped transaction when found`() = runTest {
        coEvery { dao.getTransactionById(1L) } returns sampleEntity

        val result = repository.getTransactionById(1L)

        assertEquals("Lunch", result?.description)
        assertEquals(1L, result?.id)
    }

    @Test
    fun `getTransactionById returns null when not found`() = runTest {
        coEvery { dao.getTransactionById(999L) } returns null

        val result = repository.getTransactionById(999L)

        assertNull(result)
    }

    // ---- getTransactionsByType ----

    @Test
    fun `getTransactionsByType filters correctly`() = runTest {
        val expenseEntity = sampleEntity.copy(type = TransactionType.EXPENSE)
        every { dao.getTransactionsByType(TransactionType.EXPENSE) } returns flowOf(listOf(expenseEntity))

        val result = repository.getTransactionsByType(TransactionType.EXPENSE).first()

        assertEquals(1, result.size)
        assertEquals(TransactionType.EXPENSE, result[0].type)
    }

    // ---- getTransactionsByDateRange ----

    @Test
    fun `getTransactionsByDateRange delegates to dao`() = runTest {
        val start = 1700000000000L
        val end = 1700100000000L
        every { dao.getTransactionsByDateRange(start, end) } returns flowOf(listOf(sampleEntity))

        val result = repository.getTransactionsByDateRange(start, end).first()

        assertEquals(1, result.size)
    }

    // ---- getTotalByType ----

    @Test
    fun `getTotalByType returns value from dao`() = runTest {
        every { dao.getTotalByType(TransactionType.EXPENSE) } returns flowOf(150.0)

        val result = repository.getTotalByType(TransactionType.EXPENSE).first()

        assertEquals(150.0, result, 0.001)
    }

    // ---- getBalance ----

    @Test
    fun `getBalance returns value from dao`() = runTest {
        every { dao.getBalance() } returns flowOf(500.0)

        val result = repository.getBalance().first()

        assertEquals(500.0, result, 0.001)
    }

    // ---- insertTransaction ----

    @Test
    fun `insertTransaction converts domain to entity and returns id`() = runTest {
        coEvery { dao.insert(any()) } returns 1L

        val transaction = Transaction(
            description = "Lunch",
            amount = 25.50,
            type = TransactionType.EXPENSE,
            category = "FOOD",
            date = 1700000000000L
        )

        val result = repository.insertTransaction(transaction)

        assertEquals(1L, result)
        coVerify { dao.insert(any()) }
    }

    // ---- deleteTransaction ----

    @Test
    fun `deleteTransactionById delegates to dao`() = runTest {
        repository.deleteTransactionById(1L)

        coVerify { dao.deleteById(1L) }
    }
}
