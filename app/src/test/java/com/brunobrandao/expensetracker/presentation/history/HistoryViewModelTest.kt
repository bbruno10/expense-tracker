package com.brunobrandao.expensetracker.presentation.history

import com.brunobrandao.expensetracker.data.preferences.UserPreferences
import com.brunobrandao.expensetracker.data.preferences.UserPreferencesRepository
import com.brunobrandao.expensetracker.data.sync.SyncRepository
import com.brunobrandao.expensetracker.domain.model.Transaction
import com.brunobrandao.expensetracker.domain.model.TransactionType
import com.brunobrandao.expensetracker.domain.repository.AuthRepository
import com.brunobrandao.expensetracker.domain.repository.CategoryRepository
import com.brunobrandao.expensetracker.domain.usecase.DeleteTransactionUseCase
import com.brunobrandao.expensetracker.domain.usecase.GetTransactionsUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getTransactions: GetTransactionsUseCase
    private lateinit var deleteTransaction: DeleteTransactionUseCase
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var syncRepository: SyncRepository
    private lateinit var preferencesRepository: UserPreferencesRepository
    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getTransactions = mockk(relaxed = true)
        deleteTransaction = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        syncRepository = mockk(relaxed = true)
        preferencesRepository = mockk(relaxed = true)
        every { getTransactions() } returns flowOf(emptyList())
        every { categoryRepository.observeCategories() } returns flowOf(emptyList())
        every { authRepository.currentUserId } returns "user-1"
        every { preferencesRepository.userPreferences } returns flowOf(UserPreferences())
        viewModel = HistoryViewModel(
            getTransactions, deleteTransaction, categoryRepository, authRepository, syncRepository, preferencesRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Test 1: authenticated path uses syncDelete, not the use case directly ─

    @Test
    fun `DeleteTransaction with userId calls syncDelete and not DeleteTransactionUseCase`() = runTest {
        viewModel.onEvent(HistoryEvent.DeleteTransaction(42L))
        advanceUntilIdle()

        coVerify(exactly = 1) { syncRepository.syncDelete(42L, "user-1") }
        coVerify(exactly = 0) { deleteTransaction(any()) }
    }

    // ── Test 2: offline / no session falls back to Room-only delete ───────────

    @Test
    fun `DeleteTransaction with null userId falls back to DeleteTransactionUseCase`() = runTest {
        every { authRepository.currentUserId } returns null

        viewModel.onEvent(HistoryEvent.DeleteTransaction(7L))
        advanceUntilIdle()

        coVerify(exactly = 1) { deleteTransaction(7L) }
        coVerify(exactly = 0) { syncRepository.syncDelete(any(), any()) }
    }

    // ── Test 3: syncDelete receives the exact id from the event ──────────────

    @Test
    fun `syncDelete is called with the id from the DeleteTransaction event`() = runTest {
        viewModel.onEvent(HistoryEvent.DeleteTransaction(99L))
        advanceUntilIdle()

        coVerify { syncRepository.syncDelete(99L, "user-1") }
    }

    // ── Search tests ──────────────────────────────────────────────────────────

    private fun tx(id: Long, description: String, note: String = "") = Transaction(
        id = id,
        description = description,
        amount = 10.0,
        type = TransactionType.EXPENSE,
        category = "FOOD",
        date = 0L,
        note = note
    )

    private fun viewModelWithTransactions(vararg transactions: Transaction): HistoryViewModel {
        every { getTransactions() } returns flowOf(transactions.toList())
        return HistoryViewModel(
            getTransactions, deleteTransaction, categoryRepository, authRepository, syncRepository, preferencesRepository
        )
    }

    @Test
    fun `SearchQueryChanged filters transactions by title`() = runTest {
        val vm = viewModelWithTransactions(
            tx(1L, "Netflix subscription"),
            tx(2L, "Grocery shopping")
        )
        advanceUntilIdle()

        vm.onEvent(HistoryEvent.SearchQueryChanged("netflix"))
        advanceUntilIdle()

        val ids = vm.uiState.value.transactions.map { it.id }
        assertEquals(listOf(1L), ids)
    }

    @Test
    fun `SearchQueryChanged filters transactions by note when scope is NOTE`() = runTest {
        val vm = viewModelWithTransactions(
            tx(1L, "Payment", note = "via Pix"),
            tx(2L, "Payment", note = "via credit card")
        )
        advanceUntilIdle()

        vm.onEvent(HistoryEvent.SearchScopeChanged(SearchScope.NOTE))
        vm.onEvent(HistoryEvent.SearchQueryChanged("pix"))
        advanceUntilIdle()

        val ids = vm.uiState.value.transactions.map { it.id }
        assertEquals(listOf(1L), ids)
    }

    @Test
    fun `TITLE scope does not match transactions whose query appears only in note`() = runTest {
        val vm = viewModelWithTransactions(
            tx(1L, "Lunch", note = "with colleagues"),
            tx(2L, "colleagues dinner", note = "")
        )
        advanceUntilIdle()

        vm.onEvent(HistoryEvent.SearchScopeChanged(SearchScope.TITLE))
        vm.onEvent(HistoryEvent.SearchQueryChanged("colleagues"))
        advanceUntilIdle()

        val ids = vm.uiState.value.transactions.map { it.id }
        assertEquals(listOf(2L), ids)
    }

    @Test
    fun `search is accent and case insensitive`() = runTest {
        val vm = viewModelWithTransactions(
            tx(1L, "Cartão de crédito"),
            tx(2L, "Cash")
        )
        advanceUntilIdle()

        vm.onEvent(HistoryEvent.SearchQueryChanged("CARTAO"))
        advanceUntilIdle()

        val ids = vm.uiState.value.transactions.map { it.id }
        assertEquals(listOf(1L), ids)
    }

    @Test
    fun `ClearSearch restores all transactions`() = runTest {
        val vm = viewModelWithTransactions(
            tx(1L, "Netflix"),
            tx(2L, "Grocery")
        )
        advanceUntilIdle()

        vm.onEvent(HistoryEvent.SearchQueryChanged("netflix"))
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.transactions.size)

        vm.onEvent(HistoryEvent.ClearSearch)
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.transactions.size)
        assertTrue(vm.uiState.value.searchQuery.isEmpty())
    }

    @Test
    fun `BOTH scope matches query found in either title or note`() = runTest {
        val vm = viewModelWithTransactions(
            tx(1L, "Spotify", note = ""),
            tx(2L, "Purchase", note = "spotify family"),
            tx(3L, "Unrelated", note = "nothing here")
        )
        advanceUntilIdle()

        vm.onEvent(HistoryEvent.SearchScopeChanged(SearchScope.BOTH))
        vm.onEvent(HistoryEvent.SearchQueryChanged("spotify"))
        advanceUntilIdle()

        val ids = vm.uiState.value.transactions.map { it.id }.sorted()
        assertEquals(listOf(1L, 2L), ids)
    }
}
