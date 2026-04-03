package com.example.expensetracker.presentation.add

import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.Transaction
import com.example.expensetracker.domain.model.TransactionType
import com.example.expensetracker.domain.repository.TransactionRepository
import com.example.expensetracker.domain.usecase.AddTransactionUseCase
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddTransactionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var addTransactionUseCase: AddTransactionUseCase
    private lateinit var repository: TransactionRepository
    private lateinit var viewModel: AddTransactionViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        addTransactionUseCase = mockk()
        repository = mockk(relaxed = true)
        viewModel = AddTransactionViewModel(addTransactionUseCase, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---- Initial State ----

    @Test
    fun `initial state has correct defaults`() {
        val state = viewModel.uiState.value

        assertEquals("", state.description)
        assertEquals("", state.amount)
        assertEquals(TransactionType.EXPENSE, state.type)
        assertEquals(Category.OTHER, state.category)
        assertEquals("", state.note)
        assertFalse(state.isLoading)
        assertFalse(state.isSaved)
        assertNull(state.errorMessage)
        assertNull(state.editingId)
        assertFalse(state.isEditing)
    }

    // ---- Event Handling ----

    @Test
    fun `DescriptionChanged updates description`() {
        viewModel.onEvent(AddTransactionEvent.DescriptionChanged("Lunch"))

        assertEquals("Lunch", viewModel.uiState.value.description)
    }

    @Test
    fun `AmountChanged filters non-numeric characters`() {
        viewModel.onEvent(AddTransactionEvent.AmountChanged("12.50abc"))

        assertEquals("12.50", viewModel.uiState.value.amount)
    }

    @Test
    fun `AmountChanged allows digits and dots only`() {
        viewModel.onEvent(AddTransactionEvent.AmountChanged("$1,234.56!"))

        assertEquals("1234.56", viewModel.uiState.value.amount)
    }

    @Test
    fun `AmountChanged allows empty string`() {
        viewModel.onEvent(AddTransactionEvent.AmountChanged(""))

        assertEquals("", viewModel.uiState.value.amount)
    }

    @Test
    fun `TypeChanged updates type to INCOME`() {
        viewModel.onEvent(AddTransactionEvent.TypeChanged(TransactionType.INCOME))

        assertEquals(TransactionType.INCOME, viewModel.uiState.value.type)
    }

    @Test
    fun `TypeChanged updates type to EXPENSE`() {
        viewModel.onEvent(AddTransactionEvent.TypeChanged(TransactionType.EXPENSE))

        assertEquals(TransactionType.EXPENSE, viewModel.uiState.value.type)
    }

    @Test
    fun `CategoryChanged updates category`() {
        viewModel.onEvent(AddTransactionEvent.CategoryChanged(Category.FOOD))

        assertEquals(Category.FOOD, viewModel.uiState.value.category)
    }

    @Test
    fun `DateChanged updates date`() {
        val newDate = 1700000000000L
        viewModel.onEvent(AddTransactionEvent.DateChanged(newDate))

        assertEquals(newDate, viewModel.uiState.value.date)
    }

    @Test
    fun `NoteChanged updates note`() {
        viewModel.onEvent(AddTransactionEvent.NoteChanged("Some note"))

        assertEquals("Some note", viewModel.uiState.value.note)
    }

    @Test
    fun `DismissError clears error message`() {
        // Trigger an error first
        viewModel.onEvent(AddTransactionEvent.Save)

        // Verify error exists
        assertTrue(viewModel.uiState.value.errorMessage != null)

        // Dismiss it
        viewModel.onEvent(AddTransactionEvent.DismissError)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // ---- Validation ----

    @Test
    fun `Save with empty description shows error`() {
        viewModel.onEvent(AddTransactionEvent.AmountChanged("25.00"))
        viewModel.onEvent(AddTransactionEvent.Save)

        assertEquals("Please enter a description", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `Save with blank description shows error`() {
        viewModel.onEvent(AddTransactionEvent.DescriptionChanged("   "))
        viewModel.onEvent(AddTransactionEvent.AmountChanged("25.00"))
        viewModel.onEvent(AddTransactionEvent.Save)

        assertEquals("Please enter a description", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `Save with empty amount shows error`() {
        viewModel.onEvent(AddTransactionEvent.DescriptionChanged("Lunch"))
        viewModel.onEvent(AddTransactionEvent.Save)

        assertEquals("Please enter a valid amount", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `Save with zero amount shows error`() {
        viewModel.onEvent(AddTransactionEvent.DescriptionChanged("Lunch"))
        viewModel.onEvent(AddTransactionEvent.AmountChanged("0"))
        viewModel.onEvent(AddTransactionEvent.Save)

        assertEquals("Please enter a valid amount", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `Save with invalid amount shows error`() {
        viewModel.onEvent(AddTransactionEvent.DescriptionChanged("Lunch"))
        viewModel.onEvent(AddTransactionEvent.AmountChanged("abc"))
        viewModel.onEvent(AddTransactionEvent.Save)

        // After filtering, "abc" becomes "" which is not a valid double
        assertEquals("Please enter a valid amount", viewModel.uiState.value.errorMessage)
    }

    // ---- Successful Save ----

    @Test
    fun `Save with valid data calls addTransaction and sets isSaved`() = runTest {
        coEvery { addTransactionUseCase(any()) } returns 1L

        viewModel.onEvent(AddTransactionEvent.DescriptionChanged("Lunch"))
        viewModel.onEvent(AddTransactionEvent.AmountChanged("25.50"))
        viewModel.onEvent(AddTransactionEvent.CategoryChanged(Category.FOOD))
        viewModel.onEvent(AddTransactionEvent.Save)

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        assertFalse(viewModel.uiState.value.isLoading)
        coVerify { addTransactionUseCase(any()) }
    }

    @Test
    fun `Save trims description and note`() = runTest {
        coEvery { addTransactionUseCase(any()) } returns 1L

        viewModel.onEvent(AddTransactionEvent.DescriptionChanged("  Lunch  "))
        viewModel.onEvent(AddTransactionEvent.AmountChanged("25.00"))
        viewModel.onEvent(AddTransactionEvent.NoteChanged("  some note  "))
        viewModel.onEvent(AddTransactionEvent.Save)

        advanceUntilIdle()

        coVerify {
            addTransactionUseCase(match { transaction ->
                transaction.description == "Lunch" && transaction.note == "some note"
            })
        }
    }

    // ---- Load Transaction for Editing ----

    @Test
    fun `loadTransaction with valid id populates state`() = runTest {
        val existingTransaction = Transaction(
            id = 5L,
            description = "Groceries",
            amount = 42.0,
            type = TransactionType.EXPENSE,
            category = Category.SHOPPING,
            date = 1700000000000L,
            note = "Weekly groceries"
        )

        coEvery { repository.getTransactionById(5L) } returns existingTransaction

        viewModel.loadTransaction(5L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(5L, state.editingId)
        assertEquals("Groceries", state.description)
        assertEquals("42.0", state.amount)
        assertEquals(TransactionType.EXPENSE, state.type)
        assertEquals(Category.SHOPPING, state.category)
        assertEquals("Weekly groceries", state.note)
        assertTrue(state.isEditing)
    }

    @Test
    fun `loadTransaction with zero id does nothing`() = runTest {
        viewModel.loadTransaction(0L)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.editingId)
        coVerify(exactly = 0) { repository.getTransactionById(any()) }
    }

    @Test
    fun `loadTransaction with negative id does nothing`() = runTest {
        viewModel.loadTransaction(-1L)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.editingId)
    }

    // ---- Update (Edit mode) ----

    @Test
    fun `Save in edit mode calls updateTransaction`() = runTest {
        val existingTransaction = Transaction(
            id = 5L,
            description = "Groceries",
            amount = 42.0,
            type = TransactionType.EXPENSE,
            category = Category.SHOPPING,
            date = 1700000000000L,
            note = ""
        )

        coEvery { repository.getTransactionById(5L) } returns existingTransaction
        coEvery { repository.updateTransaction(any()) } returns Unit

        viewModel.loadTransaction(5L)
        advanceUntilIdle()

        viewModel.onEvent(AddTransactionEvent.DescriptionChanged("Updated groceries"))
        viewModel.onEvent(AddTransactionEvent.Save)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        coVerify { repository.updateTransaction(any()) }
        coVerify(exactly = 0) { addTransactionUseCase(any()) }
    }
}
