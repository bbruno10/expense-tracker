package com.brunobrandao.expensetracker.presentation.history

import com.brunobrandao.expensetracker.data.preferences.UserPreferences
import com.brunobrandao.expensetracker.data.preferences.UserPreferencesRepository
import com.brunobrandao.expensetracker.data.sync.SyncRepository
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
}
