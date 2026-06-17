package com.brunobrandao.expensetracker.data.repository

import com.brunobrandao.expensetracker.data.local.dao.TransactionDao
import com.brunobrandao.expensetracker.data.local.entity.TransactionEntity
import com.brunobrandao.expensetracker.domain.model.Transaction
import com.brunobrandao.expensetracker.domain.model.TransactionType
import com.brunobrandao.expensetracker.domain.util.Clock
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

    private val fakeNow = 1717200000000L
    private val fakeClock = Clock { fakeNow }

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
        repository = TransactionRepositoryImpl(dao, fakeClock)
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

    // ---- updateTransaction — sync field preservation ----

    @Test
    fun `updateTransaction preserves remoteId and marks unsynced with clock timestamp`() = runTest {
        val existingEntity = TransactionEntity(
            id = 5L,
            description = "Old description",
            amount = 100.0,
            type = TransactionType.EXPENSE,
            category = "FOOD",
            date = 1700000000000L,
            createdAt = 1700000000000L,
            remoteId = "abc123",
            synced = true,
            updatedAt = 1700000000000L
        )
        coEvery { dao.getTransactionById(5L) } returns existingEntity

        val updatedDomain = Transaction(
            id = 5L,
            description = "New description",
            amount = 150.0,
            type = TransactionType.EXPENSE,
            category = "SHOPPING",
            date = 1700000000000L
        )

        repository.updateTransaction(updatedDomain)

        coVerify {
            dao.update(match { entity ->
                entity.id == 5L &&
                entity.description == "New description" &&
                entity.amount == 150.0 &&
                entity.category == "SHOPPING" &&
                entity.remoteId == "abc123" &&   // preservado do entity existente
                entity.synced == false &&         // marcado para re-sync
                entity.updatedAt == fakeNow       // via clock injetável, não System.currentTimeMillis()
            })
        }
    }

    @Test
    fun `updateTransaction with already-synced entity resets synced flag`() = runTest {
        val existingEntity = TransactionEntity(
            id = 7L,
            description = "Groceries",
            amount = 80.0,
            type = TransactionType.EXPENSE,
            category = "FOOD",
            date = 1700000000000L,
            createdAt = 1700000000000L,
            remoteId = "xyz789",
            synced = true,
            updatedAt = 1700000000000L
        )
        coEvery { dao.getTransactionById(7L) } returns existingEntity

        repository.updateTransaction(
            Transaction(id = 7L, description = "Groceries updated", amount = 90.0,
                type = TransactionType.EXPENSE, category = "FOOD", date = 1700000000000L)
        )

        coVerify {
            dao.update(match { entity ->
                entity.synced == false && entity.remoteId == "xyz789" && entity.updatedAt == fakeNow
            })
        }
    }

    @Test
    fun `updateTransaction when existing not found uses empty remoteId`() = runTest {
        // edge case: entity sumiu do DB entre query e update
        coEvery { dao.getTransactionById(99L) } returns null

        repository.updateTransaction(
            Transaction(id = 99L, description = "Ghost", amount = 10.0,
                type = TransactionType.EXPENSE, category = "OTHER", date = 1700000000000L)
        )

        coVerify {
            dao.update(match { entity ->
                entity.remoteId == "" && entity.synced == false && entity.updatedAt == fakeNow
            })
        }
    }
}
