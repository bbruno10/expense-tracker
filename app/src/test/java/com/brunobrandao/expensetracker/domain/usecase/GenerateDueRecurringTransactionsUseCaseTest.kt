package com.brunobrandao.expensetracker.domain.usecase

import com.brunobrandao.expensetracker.domain.model.RecurringFrequency
import com.brunobrandao.expensetracker.domain.model.RecurringTransaction
import com.brunobrandao.expensetracker.domain.model.Transaction
import com.brunobrandao.expensetracker.domain.model.TransactionType
import com.brunobrandao.expensetracker.domain.repository.RecurringTransactionRepository
import com.brunobrandao.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.TimeZone

/** Converte LocalDate (UTC meia-noite) para epoch millis. */
private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

// ─── Fakes ───────────────────────────────────────────────────────────────────

class FakeRecurringTransactionRepository : RecurringTransactionRepository {

    val rules = mutableListOf<RecurringTransaction>()

    override fun observeAll(): Flow<List<RecurringTransaction>> = flowOf(rules.toList())

    override suspend fun getById(id: Long): RecurringTransaction? =
        rules.find { it.id == id }

    override suspend fun getActiveDue(now: Long): List<RecurringTransaction> =
        rules.filter { it.active && it.nextDueDate <= now }

    override suspend fun upsert(rule: RecurringTransaction): Long {
        val idx = rules.indexOfFirst { it.id == rule.id }
        return if (idx >= 0) {
            rules[idx] = rule
            rule.id
        } else {
            val withId = rule.copy(id = (rules.maxOfOrNull { it.id } ?: 0L) + 1L)
            rules.add(withId)
            withId.id
        }
    }

    override suspend fun delete(rule: RecurringTransaction) {
        rules.removeAll { it.id == rule.id }
    }

    override suspend fun deleteById(id: Long) {
        rules.removeAll { it.id == id }
    }

    override suspend fun setActive(id: Long, active: Boolean) {
        val idx = rules.indexOfFirst { it.id == id }
        if (idx >= 0) rules[idx] = rules[idx].copy(active = active)
    }
}

class FakeTransactionRepository : TransactionRepository {

    val inserted = mutableListOf<Transaction>()

    override suspend fun getLatestByRecurringId(recurringId: Long): Transaction? = null

    override suspend fun insertTransaction(transaction: Transaction): Long {
        inserted.add(transaction)
        return inserted.size.toLong()
    }

    override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(emptyList())
    override suspend fun getTransactionById(id: Long): Transaction? = null
    override fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> = flowOf(emptyList())
    override fun getTransactionsByCategory(category: String): Flow<List<Transaction>> = flowOf(emptyList())
    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(emptyList())
    override fun getTransactionsByTypeAndDateRange(type: TransactionType, startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(emptyList())
    override fun getTotalByType(type: TransactionType): Flow<Double> = flowOf(0.0)
    override fun getTotalByTypeAndDateRange(type: TransactionType, startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
    override fun getBalance(): Flow<Double> = flowOf(0.0)
    override fun getBalanceByDateRange(startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
    override suspend fun updateTransaction(transaction: Transaction) {}
    override suspend fun deleteTransaction(transaction: Transaction) {}
    override suspend fun deleteTransactionById(id: Long) {}
}

// ─── Teste ───────────────────────────────────────────────────────────────────

class GenerateDueRecurringTransactionsUseCaseTest {

    private lateinit var recurringRepo: FakeRecurringTransactionRepository
    private lateinit var txRepo: FakeTransactionRepository
    private lateinit var useCase: GenerateDueRecurringTransactionsUseCase

    // Datas de referência — todas fixas em UTC, sem System.currentTimeMillis().
    private val jan1_2024  = LocalDate.of(2024, 1, 1).toUtcMillis()
    private val jan8_2024  = LocalDate.of(2024, 1, 8).toUtcMillis()
    private val jan14_2024 = LocalDate.of(2024, 1, 14).toUtcMillis()
    private val jan15_2024 = LocalDate.of(2024, 1, 15).toUtcMillis()
    private val jan31_2024 = LocalDate.of(2024, 1, 31).toUtcMillis()
    private val feb1_2024  = LocalDate.of(2024, 2, 1).toUtcMillis()
    private val feb29_2024 = LocalDate.of(2024, 2, 29).toUtcMillis()  // 2024 = ano bissexto
    private val mar1_2024  = LocalDate.of(2024, 3, 1).toUtcMillis()
    private val mar31_2024 = LocalDate.of(2024, 3, 31).toUtcMillis()
    private val apr1_2024  = LocalDate.of(2024, 4, 1).toUtcMillis()
    private val jan1_2023  = LocalDate.of(2023, 1, 1).toUtcMillis()
    private val jan1_2025  = LocalDate.of(2025, 1, 1).toUtcMillis()

    @Before
    fun setup() {
        recurringRepo = FakeRecurringTransactionRepository()
        txRepo = FakeTransactionRepository()
        useCase = GenerateDueRecurringTransactionsUseCase(recurringRepo, txRepo)
    }

    private fun makeRule(
        id: Long = 1L,
        frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
        nextDueDate: Long = jan1_2024,
        active: Boolean = true
    ) = RecurringTransaction(
        id = id,
        description = "Netflix",
        amount = 39.90,
        type = TransactionType.EXPENSE,
        category = "OTHER",
        note = "Streaming",
        frequency = frequency,
        startDate = nextDueDate,
        nextDueDate = nextDueDate,
        active = active
    )

    // ── 1. Sem regras vencidas ────────────────────────────────────────────────

    @Test
    fun `no due rules creates no transactions`() = runTest {
        useCase(jan31_2024)

        assertTrue(txRepo.inserted.isEmpty())
    }

    @Test
    fun `rule not yet due creates no transactions`() = runTest {
        recurringRepo.rules.add(makeRule(nextDueDate = feb1_2024))

        useCase(jan31_2024)  // now < nextDueDate

        assertTrue(txRepo.inserted.isEmpty())
    }

    // ── 2. 1 regra mensal vencida 1 vez ──────────────────────────────────────

    @Test
    fun `one monthly rule due once creates one transaction and advances nextDueDate`() = runTest {
        recurringRepo.rules.add(makeRule(nextDueDate = jan1_2024))

        useCase(jan31_2024)

        assertEquals(1, txRepo.inserted.size)
        assertEquals(jan1_2024, txRepo.inserted[0].date)
        assertEquals(feb1_2024, recurringRepo.rules[0].nextDueDate)
    }

    // ── 3. Catch-up: regra mensal sem rodar por 3 meses ──────────────────────

    @Test
    fun `monthly rule 3 months overdue creates 3 transactions with catch-up`() = runTest {
        recurringRepo.rules.add(makeRule(nextDueDate = jan1_2024))

        useCase(mar31_2024)  // jan, fev, mar estão vencidos; abr ainda não

        assertEquals(3, txRepo.inserted.size)
        assertEquals(jan1_2024, txRepo.inserted[0].date)
        assertEquals(feb1_2024, txRepo.inserted[1].date)
        assertEquals(mar1_2024, txRepo.inserted[2].date)
        assertEquals(apr1_2024, recurringRepo.rules[0].nextDueDate)
    }

    // ── 4. Regra inativa é ignorada ──────────────────────────────────────────

    @Test
    fun `inactive rule is ignored even when overdue`() = runTest {
        recurringRepo.rules.add(makeRule(nextDueDate = jan1_2024, active = false))

        useCase(mar31_2024)

        assertTrue(txRepo.inserted.isEmpty())
    }

    // ── 5. Transação gerada tem os valores corretos da regra ──────────────────

    @Test
    fun `generated transaction carries all rule fields and recurringId`() = runTest {
        recurringRepo.rules.add(makeRule(id = 42L, nextDueDate = jan1_2024))

        useCase(jan31_2024)

        val tx = txRepo.inserted[0]
        assertEquals("Netflix", tx.description)
        assertEquals(39.90, tx.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals("OTHER", tx.category)
        assertEquals("Streaming", tx.note)
        assertEquals(jan1_2024, tx.date)
        assertEquals(42L, tx.recurringId)
    }

    // ── 6a. Frequência WEEKLY ─────────────────────────────────────────────────

    @Test
    fun `weekly rule 2 weeks overdue creates 2 transactions and advances by 7 days each`() = runTest {
        recurringRepo.rules.add(makeRule(frequency = RecurringFrequency.WEEKLY, nextDueDate = jan1_2024))

        useCase(jan14_2024)  // jan1 e jan8 vencidos; jan15 ainda não

        assertEquals(2, txRepo.inserted.size)
        assertEquals(jan1_2024, txRepo.inserted[0].date)
        assertEquals(jan8_2024, txRepo.inserted[1].date)
        assertEquals(jan15_2024, recurringRepo.rules[0].nextDueDate)
    }

    // ── 6b. Frequência YEARLY ─────────────────────────────────────────────────

    @Test
    fun `yearly rule 2 years overdue creates 2 transactions and advances by 1 year each`() = runTest {
        recurringRepo.rules.add(makeRule(frequency = RecurringFrequency.YEARLY, nextDueDate = jan1_2023))

        useCase(jan1_2025 - 1)  // jan1_2023 e jan1_2024 vencidos; jan1_2025 ainda não

        assertEquals(2, txRepo.inserted.size)
        assertEquals(jan1_2023, txRepo.inserted[0].date)
        assertEquals(jan1_2024, txRepo.inserted[1].date)
        assertEquals(jan1_2025, recurringRepo.rules[0].nextDueDate)
    }

    // ── 6c. Virada de mês: 31/jan + 1 mês → 29/fev (ano bissexto) ────────────

    @Test
    fun `monthly from Jan 31 advances to Feb 29 in leap year`() = runTest {
        recurringRepo.rules.add(makeRule(nextDueDate = jan31_2024))

        // now = Feb 29 - 1ms: jan31 vence, mas feb29 ainda não
        useCase(feb29_2024 - 1)

        assertEquals(1, txRepo.inserted.size)
        assertEquals(jan31_2024, txRepo.inserted[0].date)
        // nextDueDate deve ser 29/fev/2024, não 03/mar nem algo inválido
        assertEquals(feb29_2024, recurringRepo.rules[0].nextDueDate)
    }

    // ── Regressão: fuso horário não desloca o dia ─────────────────────────────

    @Test
    fun `advanceDate stays on correct calendar day under non-UTC timezone`() = runTest {
        val jan8  = LocalDate.of(2024, 1, 8).toUtcMillis()
        val feb8  = LocalDate.of(2024, 2, 8).toUtcMillis()
        val mar8  = LocalDate.of(2024, 3, 8).toUtcMillis()
        val apr8  = LocalDate.of(2024, 4, 8).toUtcMillis()

        val originalTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"))
        try {
            recurringRepo.rules.add(makeRule(nextDueDate = jan8))

            // mar31: jan8, feb8, mar8 are due; apr8 is not
            useCase(mar31_2024)

            assertEquals(3, txRepo.inserted.size)
            assertEquals(jan8, txRepo.inserted[0].date)
            assertEquals(feb8, txRepo.inserted[1].date)
            assertEquals(mar8, txRepo.inserted[2].date)
            assertEquals(apr8, recurringRepo.rules[0].nextDueDate)
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    // ── 7. Re-ancoragem de regra com nextDueDate driftado ────────────────────

    @Test
    fun `monthly rule with drifted nextDueDate re-anchors Mar-31 from startDate not Mar-28`() = runTest {
        val jan31_2025 = LocalDate.of(2025, 1, 31).toUtcMillis()
        val feb28_2025 = LocalDate.of(2025, 2, 28).toUtcMillis()
        val mar31_2025 = LocalDate.of(2025, 3, 31).toUtcMillis()
        val apr30_2025 = LocalDate.of(2025, 4, 30).toUtcMillis()

        // Estado driftado de um build antigo: startDate=Jan-31, nextDueDate=Feb-28 (errado)
        recurringRepo.rules.add(
            RecurringTransaction(
                id = 1L,
                description = "Aluguel",
                amount = 1500.0,
                type = TransactionType.EXPENSE,
                category = "HOUSING",
                note = "",
                frequency = RecurringFrequency.MONTHLY,
                startDate = jan31_2025,
                nextDueDate = feb28_2025,
                active = true
            )
        )

        // now = Apr-30 - 1ms: Feb-28 e Mar-31 vencem; Apr-30 (meia-noite) ainda não
        useCase(apr30_2025 - 1)

        assertEquals(2, txRepo.inserted.size)
        assertEquals(feb28_2025, txRepo.inserted[0].date)
        assertEquals(mar31_2025, txRepo.inserted[1].date)  // re-ancorado! não Mar-28
        assertEquals(apr30_2025, recurringRepo.rules[0].nextDueDate)
    }

    // ── Extra: múltiplas regras independentes ─────────────────────────────────

    @Test
    fun `multiple rules are each processed independently`() = runTest {
        recurringRepo.rules.add(makeRule(id = 1L, nextDueDate = jan1_2024))
        recurringRepo.rules.add(makeRule(id = 2L, frequency = RecurringFrequency.WEEKLY, nextDueDate = jan1_2024))

        useCase(jan8_2024)

        // Mensal:  jan1 <= jan8 → cria jan1, avança fev1; fev1 > jan8 → para  (1 tx)
        // Semanal: jan1 <= jan8 → cria jan1, avança jan8; jan8 <= jan8 → cria jan8, avança jan15 (2 tx)
        assertEquals(3, txRepo.inserted.size)
        assertEquals(1, txRepo.inserted.filter { it.recurringId == 1L }.size)
        assertEquals(2, txRepo.inserted.filter { it.recurringId == 2L }.size)
    }
}
