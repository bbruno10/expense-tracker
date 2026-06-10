package com.brunobrandao.expensetracker.presentation.recurring

import com.brunobrandao.expensetracker.domain.model.Category
import com.brunobrandao.expensetracker.domain.model.RecurringFrequency
import com.brunobrandao.expensetracker.domain.model.RecurringTransaction
import com.brunobrandao.expensetracker.domain.model.TransactionType
import com.brunobrandao.expensetracker.domain.repository.RecurringTransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private lateinit var recurringRepository: RecurringTransactionRepository
    private lateinit var viewModel: EditRecurringViewModel

    // Datas fixas — sem System.currentTimeMillis() em nenhum assert.
    private val startDate = 1700000000000L
    private val nextDue  = 1702678400000L

    private val sampleRule = RecurringTransaction(
        id = 1L,
        description = "Netflix",
        amount = 39.90,
        type = TransactionType.EXPENSE,
        category = Category.OTHER,
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
        viewModel = EditRecurringViewModel(recurringRepository)
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
        assertEquals(Category.OTHER, state.category)
        assertEquals("Streaming", state.note)
        assertEquals(RecurringFrequency.MONTHLY, state.frequency)
        assertTrue(state.active)
        assertEquals(startDate, state.startDate)
        assertEquals(nextDue, state.nextDueDate)
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
                rule.nextDueDate == nextDue
            })
        }
    }

    @Test
    fun `NextDueDateChanged updates nextDueDate in state`() = runTest {
        coEvery { recurringRepository.getById(1L) } returns sampleRule
        viewModel.load(1L)
        advanceUntilIdle()

        val newDate = 1705708800000L  // fixed millis, no System.currentTimeMillis()
        viewModel.onEvent(EditRecurringEvent.NextDueDateChanged(newDate))

        assertEquals(newDate, viewModel.uiState.value.nextDueDate)
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
