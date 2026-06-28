package com.brunobrandao.expensetracker.presentation.add

import com.brunobrandao.expensetracker.data.local.dao.CategoryDao
import com.brunobrandao.expensetracker.data.local.entity.CategoryEntity
import com.brunobrandao.expensetracker.data.repository.CategoryRepositoryImpl
import com.brunobrandao.expensetracker.data.sync.SyncRepository
import com.brunobrandao.expensetracker.domain.model.RecurringFrequency
import com.brunobrandao.expensetracker.domain.model.Transaction
import com.brunobrandao.expensetracker.domain.model.TransactionType
import com.brunobrandao.expensetracker.domain.repository.AuthRepository
import com.brunobrandao.expensetracker.domain.repository.RecurringTransactionRepository
import com.brunobrandao.expensetracker.domain.repository.TransactionRepository
import com.brunobrandao.expensetracker.domain.usecase.AddTransactionUseCase
import com.brunobrandao.expensetracker.domain.usecase.CreateCategoryUseCase
import com.brunobrandao.expensetracker.domain.usecase.GenerateDueRecurringTransactionsUseCase
import com.brunobrandao.expensetracker.domain.usecase.UpdateRuleFromOccurrenceUseCase
import com.brunobrandao.expensetracker.domain.util.Clock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Uses real use cases + real CategoryRepositoryImpl + mocked CategoryDao, mirroring
 * ManageCategoriesViewModelTest. This sidesteps MockK's inability to stub Compose Color
 * (an inline value class) in its mangled JVM signature.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddTransactionViewModelTest {

    private val fakeNow = 1717200000000L
    private val fakeClock = Clock { fakeNow }
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var addTransactionUseCase: AddTransactionUseCase
    private lateinit var repository: TransactionRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var syncRepository: SyncRepository
    private lateinit var recurringRepository: RecurringTransactionRepository
    private lateinit var generateDue: GenerateDueRecurringTransactionsUseCase
    private lateinit var updateRuleFromOccurrence: UpdateRuleFromOccurrenceUseCase
    private lateinit var dao: CategoryDao
    private lateinit var viewModel: AddTransactionViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        addTransactionUseCase = mockk()
        repository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        syncRepository = mockk(relaxed = true)
        recurringRepository = mockk(relaxed = true)
        generateDue = mockk(relaxed = true)
        updateRuleFromOccurrence = mockk(relaxed = true)
        coEvery { updateRuleFromOccurrence(any()) } returns null
        dao = mockk(relaxed = true)
        coEvery { dao.getByKey(any()) } returns null
        every { dao.observeAll() } returns flowOf(emptyList())
        val categoryRepo = CategoryRepositoryImpl(dao, fakeClock)
        val createCategory = CreateCategoryUseCase(categoryRepo)
        viewModel = AddTransactionViewModel(
            addTransactionUseCase, repository, authRepository, syncRepository,
            recurringRepository, generateDue, categoryRepo, createCategory, fakeClock,
            updateRuleFromOccurrence
        )
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
        assertEquals("OTHER", state.category)
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
    fun `CategoryChanged updates category key`() {
        viewModel.onEvent(AddTransactionEvent.CategoryChanged("FOOD"))

        assertEquals("FOOD", viewModel.uiState.value.category)
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
        viewModel.onEvent(AddTransactionEvent.Save)

        assertTrue(viewModel.uiState.value.errorMessage != null)

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

        assertEquals("Please enter a valid amount", viewModel.uiState.value.errorMessage)
    }

    // ---- Successful Save ----

    @Test
    fun `Save with valid data calls addTransaction and sets isSaved`() = runTest {
        coEvery { addTransactionUseCase(any()) } returns 1L

        viewModel.onEvent(AddTransactionEvent.DescriptionChanged("Lunch"))
        viewModel.onEvent(AddTransactionEvent.AmountChanged("25.50"))
        viewModel.onEvent(AddTransactionEvent.CategoryChanged("FOOD"))
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
            category = "SHOPPING",
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
        assertEquals("SHOPPING", state.category)
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
            category = "SHOPPING",
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

    @Test
    fun `loadTransaction preserves recurringId in state`() = runTest {
        val existingTransaction = Transaction(
            id = 7L,
            description = "Netflix",
            amount = 39.90,
            type = TransactionType.EXPENSE,
            category = "OTHER",
            date = 1700000000000L,
            note = "",
            recurringId = 3L
        )

        coEvery { repository.getTransactionById(7L) } returns existingTransaction

        viewModel.loadTransaction(7L)
        advanceUntilIdle()

        assertEquals(3L, viewModel.uiState.value.recurringId)
    }

    @Test
    fun `Save in edit mode preserves recurringId on updated Transaction`() = runTest {
        val existingTransaction = Transaction(
            id = 7L,
            description = "Netflix",
            amount = 39.90,
            type = TransactionType.EXPENSE,
            category = "OTHER",
            date = 1700000000000L,
            note = "",
            recurringId = 3L
        )

        coEvery { repository.getTransactionById(7L) } returns existingTransaction
        coEvery { repository.updateTransaction(any()) } returns Unit

        viewModel.loadTransaction(7L)
        advanceUntilIdle()

        viewModel.onEvent(AddTransactionEvent.AmountChanged("45.00"))
        viewModel.onEvent(AddTransactionEvent.Save)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        coVerify {
            repository.updateTransaction(match { t -> t.recurringId == 3L })
        }
    }

    // ---- Repeat mode ----

    @Test
    fun `Repeat ON save creates recurring rule via upsert and triggers generateDue`() = runTest {
        coEvery { recurringRepository.upsert(any()) } returns 1L

        val startDate = 1700000000000L  // past date
        viewModel.onEvent(AddTransactionEvent.RepeatToggled)
        viewModel.onEvent(AddTransactionEvent.DescriptionChanged("Netflix"))
        viewModel.onEvent(AddTransactionEvent.AmountChanged("39.90"))
        viewModel.onEvent(AddTransactionEvent.TypeChanged(TransactionType.EXPENSE))
        viewModel.onEvent(AddTransactionEvent.CategoryChanged("OTHER"))
        viewModel.onEvent(AddTransactionEvent.FrequencyChanged(RecurringFrequency.MONTHLY))
        viewModel.onEvent(AddTransactionEvent.StartDateChanged(startDate))
        viewModel.onEvent(AddTransactionEvent.Save)

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        coVerify {
            recurringRepository.upsert(match { rule ->
                rule.description == "Netflix" &&
                rule.amount == 39.90 &&
                rule.frequency == RecurringFrequency.MONTHLY &&
                rule.startDate == startDate &&
                rule.nextDueDate == startDate &&  // CREATE stores startDate, generateDue advances internally
                rule.active
            })
        }
        coVerify { generateDue(fakeNow) }
        coVerify(exactly = 0) { addTransactionUseCase(any()) }
    }

    @Test
    fun `Repeat ON with startDate today stores nextDueDate as startDate so generateDue can fire`() = runTest {
        // Regression test for Bug A PASSO 3: nextDueDate must equal startDate on CREATE,
        // not computeNextDueDate (future), so the generator's WHERE nextDueDate <= now fires.
        coEvery { recurringRepository.upsert(any()) } returns 1L

        val startDateToday = fakeNow  // startDate == now (clock)
        viewModel.onEvent(AddTransactionEvent.RepeatToggled)
        viewModel.onEvent(AddTransactionEvent.DescriptionChanged("Rent"))
        viewModel.onEvent(AddTransactionEvent.AmountChanged("1200.00"))
        viewModel.onEvent(AddTransactionEvent.FrequencyChanged(RecurringFrequency.MONTHLY))
        viewModel.onEvent(AddTransactionEvent.StartDateChanged(startDateToday))
        viewModel.onEvent(AddTransactionEvent.Save)

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        coVerify {
            recurringRepository.upsert(match { rule ->
                rule.startDate == startDateToday &&
                rule.nextDueDate == startDateToday  // NOT future — generator must see nextDueDate <= now
            })
        }
        coVerify { generateDue(fakeNow) }
    }

    @Test
    fun `Repeat ON with future startDate stores nextDueDate as startDate and generateDue is called`() = runTest {
        coEvery { recurringRepository.upsert(any()) } returns 1L

        val futureStart = fakeNow + 30L * 24 * 60 * 60 * 1000  // 30 days ahead
        viewModel.onEvent(AddTransactionEvent.RepeatToggled)
        viewModel.onEvent(AddTransactionEvent.DescriptionChanged("Insurance"))
        viewModel.onEvent(AddTransactionEvent.AmountChanged("200.00"))
        viewModel.onEvent(AddTransactionEvent.FrequencyChanged(RecurringFrequency.MONTHLY))
        viewModel.onEvent(AddTransactionEvent.StartDateChanged(futureStart))
        viewModel.onEvent(AddTransactionEvent.Save)

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        coVerify {
            recurringRepository.upsert(match { rule ->
                rule.startDate == futureStart &&
                rule.nextDueDate == futureStart  // future — generateDue(now) won't fire since nextDueDate > now
            })
        }
        coVerify { generateDue(fakeNow) }
    }

    @Test
    fun `Repeat OFF save creates single transaction not recurring rule`() = runTest {
        coEvery { addTransactionUseCase(any()) } returns 1L

        viewModel.onEvent(AddTransactionEvent.DescriptionChanged("Lunch"))
        viewModel.onEvent(AddTransactionEvent.AmountChanged("25.00"))
        viewModel.onEvent(AddTransactionEvent.Save)

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        coVerify { addTransactionUseCase(any()) }
        coVerify(exactly = 0) { recurringRepository.upsert(any()) }
        coVerify(exactly = 0) { generateDue(any()) }
    }

    // ---- New Category Dialog ----

    @Test
    fun `ShowNewCategoryDialog opens form with defaults`() {
        viewModel.onEvent(AddTransactionEvent.ShowNewCategoryDialog)

        val form = viewModel.uiState.value.newCategoryForm
        assertFalse(form == null)
        assertEquals("", form!!.name)
        assertNull(form.nameError)
    }

    @Test
    fun `DismissNewCategoryDialog closes dialog without changing category`() {
        viewModel.onEvent(AddTransactionEvent.CategoryChanged("FOOD"))
        viewModel.onEvent(AddTransactionEvent.ShowNewCategoryDialog)
        viewModel.onEvent(AddTransactionEvent.DismissNewCategoryDialog)

        assertNull(viewModel.uiState.value.newCategoryForm)
        assertEquals("FOOD", viewModel.uiState.value.category)
    }

    @Test
    fun `SaveNewCategory success auto-selects created category and closes dialog`() = runTest {
        // Capture the entity upserted into the DAO to get the generated UUID key.
        val upsertSlot = slot<CategoryEntity>()
        coEvery { dao.upsert(capture(upsertSlot)) } returns Unit

        viewModel.onEvent(AddTransactionEvent.ShowNewCategoryDialog)
        viewModel.onEvent(AddTransactionEvent.NewCategoryNameChanged("Hobbies"))
        viewModel.onEvent(AddTransactionEvent.SaveNewCategory)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.newCategoryForm)
        assertEquals(upsertSlot.captured.key, viewModel.uiState.value.category)
    }

    @Test
    fun `SaveNewCategory preserves rest of form after creating category`() = runTest {
        val upsertSlot = slot<CategoryEntity>()
        coEvery { dao.upsert(capture(upsertSlot)) } returns Unit

        viewModel.onEvent(AddTransactionEvent.DescriptionChanged("Board game"))
        viewModel.onEvent(AddTransactionEvent.AmountChanged("35.00"))
        viewModel.onEvent(AddTransactionEvent.TypeChanged(TransactionType.EXPENSE))
        viewModel.onEvent(AddTransactionEvent.ShowNewCategoryDialog)
        viewModel.onEvent(AddTransactionEvent.NewCategoryNameChanged("Hobbies"))
        viewModel.onEvent(AddTransactionEvent.SaveNewCategory)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Board game", state.description)
        assertEquals("35.00", state.amount)
        assertEquals(TransactionType.EXPENSE, state.type)
        assertEquals(upsertSlot.captured.key, state.category)
        assertNull(state.newCategoryForm)
    }

    @Test
    fun `SaveNewCategory with empty name surfaces nameError without closing dialog`() = runTest {
        // Empty name is rejected by CreateCategoryUseCase before any DAO call.
        viewModel.onEvent(AddTransactionEvent.ShowNewCategoryDialog)
        viewModel.onEvent(AddTransactionEvent.NewCategoryNameChanged(""))
        viewModel.onEvent(AddTransactionEvent.SaveNewCategory)
        advanceUntilIdle()

        val form = viewModel.uiState.value.newCategoryForm
        assertFalse(form == null)
        assertTrue(form!!.nameError!!.contains("empty"))
        assertEquals("OTHER", viewModel.uiState.value.category)
    }

    @Test
    fun `SaveNewCategory with duplicate name surfaces nameError without closing dialog`() = runTest {
        val foodEntity = CategoryEntity(
            key = "food-key", name = "Food", icon = "🍔",
            colorArgb = 0xFFE53935.toInt(), lightColorArgb = 0xFFFCEBEB.toInt(),
            isDefault = false, position = 0, archived = false
        )
        coEvery { dao.getActive() } returns listOf(foodEntity)

        viewModel.onEvent(AddTransactionEvent.ShowNewCategoryDialog)
        viewModel.onEvent(AddTransactionEvent.NewCategoryNameChanged("Food"))
        viewModel.onEvent(AddTransactionEvent.SaveNewCategory)
        advanceUntilIdle()

        val form = viewModel.uiState.value.newCategoryForm
        assertFalse(form == null)
        assertTrue(form!!.nameError!!.contains("already exists"))
        assertEquals("OTHER", viewModel.uiState.value.category)
    }
}
