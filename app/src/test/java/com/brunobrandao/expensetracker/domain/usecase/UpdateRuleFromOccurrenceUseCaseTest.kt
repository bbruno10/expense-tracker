package com.brunobrandao.expensetracker.domain.usecase

import com.brunobrandao.expensetracker.domain.model.RecurringFrequency
import com.brunobrandao.expensetracker.domain.model.RecurringTransaction
import com.brunobrandao.expensetracker.domain.model.Transaction
import com.brunobrandao.expensetracker.domain.model.TransactionType
import com.brunobrandao.expensetracker.domain.repository.RecurringTransactionRepository
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

// ─── Fakes ───────────────────────────────────────────────────────────────────

class FakeRecurringRepoForRuleUpdate : RecurringTransactionRepository {

    val store = mutableMapOf<Long, RecurringTransaction>()
    val upserted = mutableListOf<RecurringTransaction>()

    fun seed(vararg rules: RecurringTransaction) = rules.forEach { store[it.id] = it }

    override suspend fun getById(id: Long): RecurringTransaction? = store[id]

    override suspend fun upsert(rule: RecurringTransaction): Long {
        store[rule.id] = rule
        upserted.add(rule)
        return rule.id
    }

    override fun observeAll(): Flow<List<RecurringTransaction>> = flowOf(store.values.toList())
    override suspend fun getActiveDue(now: Long): List<RecurringTransaction> = emptyList()
    override suspend fun delete(rule: RecurringTransaction) { store.remove(rule.id) }
    override suspend fun deleteById(id: Long) { store.remove(id) }
    override suspend fun setActive(id: Long, active: Boolean) {
        store[id]?.let { store[id] = it.copy(active = active) }
    }
}

class FakeTxRepoForRuleUpdate : TransactionRepository {

    private val store = mutableMapOf<Long, Transaction>()

    fun seed(vararg transactions: Transaction) = transactions.forEach { store[it.id] = it }

    override suspend fun getLatestByRecurringId(recurringId: Long): Transaction? =
        store.values.filter { it.recurringId == recurringId }.maxByOrNull { it.date }

    override suspend fun getTransactionById(id: Long): Transaction? = store[id]
    override suspend fun insertTransaction(transaction: Transaction): Long {
        val id = (store.keys.maxOrNull() ?: 0L) + 1L
        store[id] = transaction.copy(id = id)
        return id
    }
    override suspend fun updateTransaction(transaction: Transaction) { store[transaction.id] = transaction }
    override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(store.values.toList())
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

class UpdateRuleFromOccurrenceUseCaseTest {

    private lateinit var recurringRepo: FakeRecurringRepoForRuleUpdate
    private lateinit var txRepo: FakeTxRepoForRuleUpdate
    private lateinit var useCase: UpdateRuleFromOccurrenceUseCase

    // Fixed UTC dates
    private fun utc(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private val jan15 = utc(2024, 1, 15)
    private val feb15 = utc(2024, 2, 15)
    private val mar15 = utc(2024, 3, 15)
    private val apr5  = utc(2024, 4, 5)   // "now"
    private val apr15 = utc(2024, 4, 15)  // expected nextDueDate when occurrence.date = mar15

    // Legacy constants kept for null-related tests
    private val baseDate  = utc(2024, 1, 1)
    private val laterDate = utc(2024, 1, 2)

    private val fakeClock = Clock { apr5 }

    private fun makeRule(
        id: Long = 1L,
        description: String = "Netflix",
        amount: Double = 39.90,
        type: TransactionType = TransactionType.EXPENSE,
        category: String = "STREAMING",
        note: String = "Monthly",
        frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
        startDate: Long = jan15,
        nextDueDate: Long = apr15,
        active: Boolean = true
    ) = RecurringTransaction(
        id = id,
        description = description,
        amount = amount,
        type = type,
        category = category,
        note = note,
        frequency = frequency,
        startDate = startDate,
        nextDueDate = nextDueDate,
        active = active
    )

    private fun makeOccurrence(
        id: Long,
        recurringId: Long,
        date: Long = baseDate,
        description: String = "Old Netflix",
        amount: Double = 29.90,
        type: TransactionType = TransactionType.EXPENSE,
        category: String = "OTHER",
        note: String = "Old note"
    ) = Transaction(
        id = id,
        description = description,
        amount = amount,
        type = type,
        category = category,
        date = date,
        note = note,
        recurringId = recurringId
    )

    @Before
    fun setup() {
        recurringRepo = FakeRecurringRepoForRuleUpdate()
        txRepo = FakeTxRepoForRuleUpdate()
        useCase = UpdateRuleFromOccurrenceUseCase(recurringRepo, txRepo, fakeClock)
    }

    // ── Content propagation ───────────────────────────────────────────────────

    @Test
    fun `propagates content from current occurrence to rule`() = runTest {
        val rule = makeRule(id = 1L, description = "Netflix", amount = 39.90, category = "STREAMING", note = "Monthly")
        recurringRepo.seed(rule)
        val occurrence = makeOccurrence(id = 10L, recurringId = 1L, date = mar15,
            description = "Netflix HD", amount = 55.90, category = "ENTERTAINMENT", note = "Upgraded")
        txRepo.seed(occurrence)

        useCase(occurrence)

        val saved = recurringRepo.upserted.single()
        assertEquals("Netflix HD", saved.description)
        assertEquals(55.90, saved.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, saved.type)
        assertEquals("ENTERTAINMENT", saved.category)
        assertEquals("Upgraded", saved.note)
    }

    // ── Date propagation ──────────────────────────────────────────────────────

    @Test
    fun `updates startDate to occurrence date`() = runTest {
        recurringRepo.seed(makeRule(id = 1L, startDate = jan15))
        val occurrence = makeOccurrence(id = 10L, recurringId = 1L, date = mar15)
        txRepo.seed(occurrence)

        useCase(occurrence)

        assertEquals(mar15, recurringRepo.upserted.single().startDate)
    }

    @Test
    fun `updates nextDueDate to next period after occurrence date`() = runTest {
        // occurrence.date = mar15; fakeNow = apr5; frequency = MONTHLY
        // computeNextDueDate(mar15, MONTHLY, apr5): advance(mar15) = apr15 > apr5 → apr15
        recurringRepo.seed(makeRule(id = 1L))
        val occurrence = makeOccurrence(id = 10L, recurringId = 1L, date = mar15)
        txRepo.seed(occurrence)

        useCase(occurrence)

        assertEquals(apr15, recurringRepo.upserted.single().nextDueDate)
    }

    @Test
    fun `edits past occurrence date does not change rule (not most recent)`() = runTest {
        recurringRepo.seed(makeRule(id = 1L))
        val older = makeOccurrence(id = 1L, recurringId = 1L, date = jan15)
        val newer = makeOccurrence(id = 2L, recurringId = 1L, date = mar15)
        txRepo.seed(older, newer)

        // editing older occurrence's date should be a no-op
        val olderWithNewDate = older.copy(date = feb15)
        val result = useCase(olderWithNewDate)

        assertNull(result)
        assertEquals(0, recurringRepo.upserted.size)
    }

    // ── Structural fields preserved ───────────────────────────────────────────

    @Test
    fun `preserves frequency, active and id of rule`() = runTest {
        val rule = makeRule(
            id = 1L,
            frequency = RecurringFrequency.WEEKLY,
            active = false
        )
        recurringRepo.seed(rule)
        val occurrence = makeOccurrence(id = 10L, recurringId = 1L, date = baseDate)
        txRepo.seed(occurrence)

        useCase(occurrence)

        val saved = recurringRepo.upserted.single()
        assertEquals(1L, saved.id)
        assertEquals(RecurringFrequency.WEEKLY, saved.frequency)
        assertEquals(false, saved.active)
    }

    // ── Null / missing cases ──────────────────────────────────────────────────

    @Test
    fun `returns null and does not upsert when recurringId is null`() = runTest {
        val transaction = makeOccurrence(id = 5L, recurringId = 0L).copy(recurringId = null)

        val result = useCase(transaction)

        assertNull(result)
        assertEquals(0, recurringRepo.upserted.size)
    }

    @Test
    fun `returns null and does not upsert when transaction is not the most recent occurrence`() = runTest {
        recurringRepo.seed(makeRule(id = 1L))
        val older = makeOccurrence(id = 1L, recurringId = 1L, date = baseDate)
        val newer = makeOccurrence(id = 2L, recurringId = 1L, date = laterDate)
        txRepo.seed(older, newer)

        val result = useCase(older)

        assertNull(result)
        assertEquals(0, recurringRepo.upserted.size)
    }

    @Test
    fun `returns null when rule does not exist`() = runTest {
        val occurrence = makeOccurrence(id = 10L, recurringId = 99L, date = baseDate)
        txRepo.seed(occurrence)

        val result = useCase(occurrence)

        assertNull(result)
        assertEquals(0, recurringRepo.upserted.size)
    }

    @Test
    fun `returns rule id when propagation succeeds`() = runTest {
        recurringRepo.seed(makeRule(id = 7L))
        val occurrence = makeOccurrence(id = 10L, recurringId = 7L, date = baseDate)
        txRepo.seed(occurrence)

        val result = useCase(occurrence)

        assertEquals(7L, result)
    }
}
