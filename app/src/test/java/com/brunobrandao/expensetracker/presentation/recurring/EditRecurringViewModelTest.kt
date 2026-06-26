package com.brunobrandao.expensetracker.presentation.recurring

import com.brunobrandao.expensetracker.domain.model.RecurringFrequency
import com.brunobrandao.expensetracker.domain.model.RecurringTransaction
import com.brunobrandao.expensetracker.domain.model.TransactionType
import com.brunobrandao.expensetracker.data.sync.SyncRepository
import com.brunobrandao.expensetracker.domain.repository.AuthRepository
import com.brunobrandao.expensetracker.domain.repository.CategoryRepository
import com.brunobrandao.expensetracker.domain.repository.RecurringTransactionRepository
import com.brunobrandao.expensetracker.domain.util.Clock
import io.mockk.coEvery
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditRecurringViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeNow = 1717200000000L
    private val fakeClock = Clock { fakeNow }
    private lateinit var recurringRepository: RecurringTransactionRepository
    private lateinit var syncRepository: SyncRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var viewModel: EditRecurringViewModel

    private val startDate = 1700000000000L
    private val nextDue  = 1702678400000L

    private val sampleRule = RecurringTransaction(
        id = 1L,
        description = "Netflix",
        amount = 39.90,
        type = TransactionType.EXPENSE,
        category = "OTHER",
        note = "Streaming",
        frequency = RecurringFrequency.MONTHLY,
        startDate = startDate,
        nextDueDate = nextDue,
        active = true
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        recurringRepository = mockk(relaxed = true)
        syncRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        every { authRepository.currentUserId } returns null
        every { categoryRepository.observeCategories() } returns flowOf(emptyList())
        viewModel = EditRecurringViewModel(recurringRepository, syncRepository, authRepository, categoryRepository, fakeClock)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load populates state with correct values`() = runTest {
        coEvery { recurringRepository.getById(1L) } returns sampleRule

        viewModel.load(1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1L, state.id)
        assertEquals("Netflix", state.description)
        assertEquals("39.9", state.amount)
        assertEquals(TransactionType.EXPENSE, state.type)
        assertEquals("OTHER", state.category)
        assertEquals("Streaming", state.note)
        assertEquals(RecurringFrequency.MONTHLY, state.frequency)
        assertTrue(state.active)
        assertEquals(startDate, state.startDate)
    }

    @Test
    fun `save with blank description shows error and does not call upsert`() = runTest {
        coEvery { recurringRepository.getById(1L) } returns sampleRule
        viewModel.load(1L)
        advanceUntilIdle()

        viewModel.onEvent(EditRecurringEvent.DescriptionChanged("   "))
        viewModel.onEvent(EditRecurringEvent.Save)

        assertEquals("Please enter a description", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSaved)
        coVerify(exactly = 0) { recurringRepository.upsert(any()) }
    }

    @Test
    fun `save with zero amount shows error and does not call upsert`() = runTest {
        coEvery { recurringRepository.getById(1L) } returns sampleRule
        viewModel.load(1L)
        advanceUntilIdle()

        viewModel.onEvent(EditRecurringEvent.AmountChanged("0"))
        viewModel.onEvent(EditRecurringEvent.Save)

        assertEquals("Please enter a valid amount", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `save with valid data calls upsert preserving id and dates`() = runTest {
        coEvery { recurringRepository.getById(1L) } returns sampleRule
        coEvery { recurringRepository.upsert(any()) } returns 1L
        viewModel.load(1L)
        advanceUntilIdle()

        viewModel.onEvent(EditRecurringEvent.DescriptionChanged("Netflix Updated"))
        viewModel.onEvent(EditRecurringEvent.AmountChanged("49.90"))
        viewModel.onEvent(EditRecurringEvent.Save)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        coVerify {
            recurringRepository.upsert(match { rule ->
                rule.id == 1L &&
                rule.description == "Netflix Updated" &&
                rule.amount == 49.90 &&
                rule.startDate == startDate &&
                rule.nextDueDate > fakeNow
            })
        }
    }

    @Test
    fun `StartDateChanged updates startDate in state`() = runTest {
        coEvery { recurringRepository.getById(1L) } returns sampleRule
        viewModel.load(1L)
        advanceUntilIdle()

        val newDate = 1705708800000L
        viewModel.onEvent(EditRecurringEvent.StartDateChanged(newDate))

        assertEquals(newDate, viewModel.uiState.value.startDate)
    }

    @Test
    fun `save recalculates nextDueDate strictly after now`() = runTest {
        coEvery { recurringRepository.getById(1L) } returns sampleRule
        coEvery { recurringRepository.upsert(any()) } returns 1L
        viewModel.load(1L)
        advanceUntilIdle()

        viewModel.onEvent(EditRecurringEvent.Save)
        advanceUntilIdle()

        coVerify {
            recurringRepository.upsert(match { rule -> rule.nextDueDate > fakeNow })
        }
    }

    @Test
    fun `delete calls deleteById and sets isDeleted`() = runTest {
        coEvery { recurringRepository.getById(1L) } returns sampleRule
        viewModel.load(1L)
        advanceUntilIdle()

        viewModel.onEvent(EditRecurringEvent.Delete)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isDeleted)
        coVerify { recurringRepository.deleteById(1L) }
    }
}
