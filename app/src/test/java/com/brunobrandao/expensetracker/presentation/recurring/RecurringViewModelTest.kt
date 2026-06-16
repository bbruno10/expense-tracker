package com.brunobrandao.expensetracker.presentation.recurring

import com.brunobrandao.expensetracker.data.sync.SyncRepository
import com.brunobrandao.expensetracker.domain.repository.AuthRepository
import com.brunobrandao.expensetracker.domain.repository.RecurringTransactionRepository
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
class RecurringViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var recurringRepository: RecurringTransactionRepository
    private lateinit var syncRepository: SyncRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: RecurringViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        recurringRepository = mockk(relaxed = true)
        syncRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        every { authRepository.currentUserId } returns null
        every { recurringRepository.observeAll() } returns flowOf(emptyList())
        viewModel = RecurringViewModel(recurringRepository, syncRepository, authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setActive delegates to repository with correct args`() = runTest {
        viewModel.setActive(5L, false)
        advanceUntilIdle()

        coVerify { recurringRepository.setActive(5L, false) }
    }

    @Test
    fun `deleteById delegates to repository`() = runTest {
        viewModel.deleteById(10L)
        advanceUntilIdle()

        coVerify { recurringRepository.deleteById(10L) }
    }
}
