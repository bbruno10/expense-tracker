package com.brunobrandao.expensetracker.domain.usecase

import com.brunobrandao.expensetracker.domain.model.RecurringFrequency
import com.brunobrandao.expensetracker.domain.model.RecurringTransaction
import com.brunobrandao.expensetracker.domain.model.Transaction
import com.brunobrandao.expensetracker.domain.model.TransactionType
import com.brunobrandao.expensetracker.domain.repository.TransactionRepository
import com.brunobrandao.expensetracker.domain.util.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

// ─── Fake ────────────────────────────────────────────────────────────────────

class FakeTransactionRepositoryForOccurrence : TransactionRepository {

    private val store = mutableMapOf<Long, Transaction>()
    val updated = mutableListOf<Transaction>()

    fun seed(vararg transactions: Transaction) {
        transactions.forEach { store[it.id] = it }
    }

    override suspend fun getLatestByRecurringId(recurringId: Long): Transaction? =
        store.values.filter { it.recurringId == recurringId }.maxByOrNull { it.date }

    override suspend fun updateTransaction(transaction: Transaction) {
        store[transaction.id] = transaction
        updated.add(transaction)
    }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        val id = (store.keys.maxOrNull() ?: 0L) + 1L
        store[id] = transaction.copy(id = id)
        return id
    }

    override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(store.values.toList())
    override suspend fun getTransactionById(id: Long): Transaction? = store[id]
    override fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> = flowOf(emptyList())
    override fun getTransactionsByCategory(category: String): Flow<List<Transaction>> = flowOf(emptyList())
    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(emptyList())
    override fun getTransactionsByTypeAndDateRange(type: TransactionType, startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(emptyList())
    override fun getTotalByType(type: TransactionType): Flow<Double> = flowOf(0.0)
    override fun getTotalByTypeAndDateRange(type: TransactionType, startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
    override fun getBalance(): Flow<Double> = flowOf(0.0)
    override fun getBalanceByDateRange(startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
    override suspend fun deleteTransaction(transaction: Transaction) { store.remove(transaction.id) }
    override suspend fun deleteTransactionById(id: Long) { store.remove(id) }
}

// ─── Tests ───────────────────────────────────────────────────────────────────

class UpdateLatestRecurringOccurrenceUseCaseTest {

    private lateinit var repo: FakeTransactionRepositoryForOccurrence
    private lateinit var useCase: UpdateLatestRecurringOccurrenceUseCase

    // Fixed dates — all UTC midnight
    private fun utc(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private val jan1  = utc(2024, 1, 1)
    private val jan15 = utc(2024, 1, 15)
    private val feb15 = utc(2024, 2, 15)
    private val mar15 = utc(2024, 3, 15)
    private val apr5  = utc(2024, 4, 5)   // "now" — between Mar-15 and Apr-15
    private val jul15 = utc(2024, 7, 15)  // future relative to apr5

    // Legacy constants kept for existing tests
    private val baseDate  = jan1
    private val laterDate = utc(2024, 1, 2)  // 1 day after jan1, same month

    private val fakeClock = Clock { apr5 }

    private fun makeRule(
        id: Long = 1L,
        description: String = "Netflix",
        amount: Double = 39.90,
        type: TransactionType = TransactionType.EXPENSE,
        category: String = "STREAMING",
        note: String = "Monthly",
        frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
        startDate: Long = jan15
    ) = RecurringTransaction(
        id = id,
        description = description,
        amount = amount,
        type = type,
        category = category,
        note = note,
        frequency = frequency,
        startDate = startDate,
        nextDueDate = apr5,
        active = true
    )

    private fun makeOccurrence(
        id: Long,
        recurringId: Long,
        date: Long = baseDate,
        description: String = "Old Netflix",
        amount: Double = 29.90,
        type: TransactionType = TransactionType.EXPENSE,
        category: String = "OTHER",
        note: String = "Old note",
        createdAt: Long = baseDate
    ) = Transaction(
        id = id,
        description = description,
        amount = amount,
        type = type,
        category = category,
        date = date,
        note = note,
        createdAt = createdAt,
        recurringId = recurringId
    )

    @Before
    fun setup() {
        repo = FakeTransactionRepositoryForOccurrence()
        useCase = UpdateLatestRecurringOccurrenceUseCase(repo, fakeClock)
    }

    // ── Content propagation ───────────────────────────────────────────────────

    @Test
    fun `propagates content fields from rule to most recent occurrence`() = runTest {
        val occurrence = makeOccurrence(id = 10L, recurringId = 1L)
        repo.seed(occurrence)

        val rule = makeRule(
            id = 1L,
            description = "Netflix Updated",
            amount = 49.90,
            type = TransactionType.EXPENSE,
            category = "STREAMING",
            note = "New plan"
        )

        useCase(rule)

        val saved = repo.updated.single()
        assertEquals("Netflix Updated", saved.description)
        assertEquals(49.90, saved.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, saved.type)
        assertEquals("STREAMING", saved.category)
        assertEquals("New plan", saved.note)
    }

    // ── Date propagation ──────────────────────────────────────────────────────

    @Test
    fun `date is updated to current period of rule`() = runTest {
        // rule.startDate = jan15; fakeNow = apr5 → currentOccurrence = mar15
        val occurrence = makeOccurrence(id = 10L, recurringId = 1L, date = jan15)
        repo.seed(occurrence)

        useCase(makeRule(id = 1L, startDate = jan15))

        assertEquals(mar15, repo.updated.single().date)
    }

    @Test
    fun `date unchanged when rule startDate is in the future`() = runTest {
        // startDate = jul15 > apr5 (fakeNow) → currentOccurrenceDate returns null → keep existing date
        val occurrence = makeOccurrence(id = 10L, recurringId = 1L, date = feb15)
        repo.seed(occurrence)

        useCase(makeRule(id = 1L, startDate = jul15))

        assertEquals(feb15, repo.updated.single().date)
    }

    // ── Non-date fields preserved ─────────────────────────────────────────────

    @Test
    fun `preserves id, createdAt and recurringId of existing occurrence`() = runTest {
        val createdAt = baseDate - 1000L
        val occurrence = makeOccurrence(id = 10L, recurringId = 1L, date = laterDate, createdAt = createdAt)
        repo.seed(occurrence)

        useCase(makeRule(id = 1L))

        val saved = repo.updated.single()
        assertEquals(10L, saved.id)
        assertEquals(createdAt, saved.createdAt)
        assertEquals(1L, saved.recurringId)
    }

    // ── Null / missing cases ──────────────────────────────────────────────────

    @Test
    fun `returns null when no occurrence is linked to the rule`() = runTest {
        val result = useCase(makeRule(id = 99L))

        assertNull(result)
    }

    @Test
    fun `returns updated occurrence id when occurrence exists`() = runTest {
        repo.seed(makeOccurrence(id = 10L, recurringId = 1L))

        val result = useCase(makeRule(id = 1L))

        assertEquals(10L, result)
    }

    // ── Most-recent-only ──────────────────────────────────────────────────────

    @Test
    fun `when multiple occurrences exist only the most recent by date is updated`() = runTest {
        val older = makeOccurrence(id = 1L, recurringId = 5L, date = baseDate, description = "Old Jan")
        val newer = makeOccurrence(id = 2L, recurringId = 5L, date = laterDate, description = "Old Feb")
        repo.seed(older, newer)

        val rule = makeRule(id = 5L, description = "Updated Feb")

        val result = useCase(rule)

        assertEquals(2L, result)
        assertEquals(1, repo.updated.size)
        assertEquals(2L, repo.updated[0].id)
        assertEquals("Updated Feb", repo.updated[0].description)
        // older occurrence untouched
        assertEquals("Old Jan", repo.getTransactionById(1L)?.description)
    }
}
