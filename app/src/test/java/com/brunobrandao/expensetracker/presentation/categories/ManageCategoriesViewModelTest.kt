package com.brunobrandao.expensetracker.presentation.categories

import androidx.compose.ui.graphics.Color
import com.brunobrandao.expensetracker.data.local.dao.CategoryDao
import com.brunobrandao.expensetracker.data.local.entity.CategoryEntity
import com.brunobrandao.expensetracker.data.repository.CategoryRepositoryImpl
import com.brunobrandao.expensetracker.domain.model.Category
import com.brunobrandao.expensetracker.domain.usecase.ArchiveCategoryUseCase
import com.brunobrandao.expensetracker.domain.usecase.CreateCategoryUseCase
import com.brunobrandao.expensetracker.domain.usecase.DeleteCategoryUseCase
import com.brunobrandao.expensetracker.domain.usecase.UnarchiveCategoryUseCase
import com.brunobrandao.expensetracker.domain.usecase.UpdateCategoryUseCase
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ViewModel tests for ManageCategoriesViewModel.
 *
 * Strategy: real use cases + real CategoryRepositoryImpl + mock CategoryDao + fake Clock.
 * This sidesteps mockk's inability to stub methods with Kotlin value-class (Color) parameters
 * in their mangled JVM signature (invoke-OoHUuok). The DAO layer only deals with Int/String/Long,
 * so mocking it is straightforward.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManageCategoriesViewModelTest {

    private val fakeNow = 1717200000000L
    private val fakeClock = Clock { fakeNow }
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var dao: CategoryDao
    private lateinit var viewModel: ManageCategoriesViewModel

    // ── Entity fixtures (DAO layer — no Color value class) ────────────────────
    private val entityFood = entity("FOOD", "Food", isDefault = true, archived = false)
    private val entitySalary = entity("SALARY", "Salary", isDefault = true, archived = false)
    private val entityCustomActive = entity("custom-1", "Hobbies", isDefault = false, archived = false)
    private val entityCustomArchived = entity("custom-2", "Gym", isDefault = false, archived = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        dao = mockk(relaxed = true)

        // Default DAO stubs
        coEvery { dao.getByKey(any()) } returns null
        every { dao.observeAll() } returns flowOf(
            listOf(entityFood, entitySalary, entityCustomActive, entityCustomArchived)
        )

        val repo = CategoryRepositoryImpl(dao, fakeClock)
        viewModel = ManageCategoriesViewModel(
            categoryRepository = repo,
            createCategory    = CreateCategoryUseCase(repo),
            updateCategory    = UpdateCategoryUseCase(repo),
            archiveCategory   = ArchiveCategoryUseCase(repo),
            unarchiveCategory = UnarchiveCategoryUseCase(repo),
            deleteCategory    = DeleteCategoryUseCase(repo)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Observe / split categories ────────────────────────────────────────────

    @Test
    fun `init splits categories into active and archived lists`() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals(3, state.activeCategories.size)   // FOOD, SALARY, Hobbies
        assertEquals(1, state.archivedCategories.size)  // Gym
        assertFalse(state.isLoading)
    }

    // ── Toggle archived view ──────────────────────────────────────────────────

    @Test
    fun `ToggleShowArchived flips showArchived flag`() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showArchived)

        viewModel.onEvent(ManageCategoriesEvent.ToggleShowArchived)
        assertTrue(viewModel.uiState.value.showArchived)

        viewModel.onEvent(ManageCategoriesEvent.ToggleShowArchived)
        assertFalse(viewModel.uiState.value.showArchived)
    }

    // ── Create dialog ─────────────────────────────────────────────────────────

    @Test
    fun `ShowCreateDialog opens form with empty state and null editingKey`() = runTest {
        advanceUntilIdle()
        viewModel.onEvent(ManageCategoriesEvent.ShowCreateDialog)

        val form = viewModel.uiState.value.form
        assertNotNull(form)
        assertNull(form!!.editingKey)
        assertEquals("", form.name)
        assertNull(form.nameError)
    }

    @Test
    fun `DismissDialog closes form`() = runTest {
        advanceUntilIdle()
        viewModel.onEvent(ManageCategoriesEvent.ShowCreateDialog)
        viewModel.onEvent(ManageCategoriesEvent.DismissDialog)

        assertNull(viewModel.uiState.value.form)
    }

    // ── Successful create ─────────────────────────────────────────────────────

    @Test
    fun `SaveForm create calls use case and closes dialog on success`() = runTest {
        // dao.getActive() returns empty → no duplicate; dao.getByKey returns null → new UUID; dao.upsert → relaxed
        coEvery { dao.getActive() } returns emptyList()

        advanceUntilIdle()
        viewModel.onEvent(ManageCategoriesEvent.ShowCreateDialog)
        viewModel.onEvent(ManageCategoriesEvent.FormNameChanged("Lazer"))
        viewModel.onEvent(ManageCategoriesEvent.FormIconChanged("🎮"))
        viewModel.onEvent(ManageCategoriesEvent.FormColorChanged(5))
        viewModel.onEvent(ManageCategoriesEvent.SaveForm)
        advanceUntilIdle()

        assertNull("Form should close after successful create", viewModel.uiState.value.form)
        assertNull("No snackbar error expected", viewModel.uiState.value.snackbarMessage)
        // Verify DAO received a new entity
        coVerify(exactly = 1) { dao.upsert(any()) }
    }

    // ── Validation errors surface in form state ───────────────────────────────

    @Test
    fun `SaveForm create with duplicate name surfaces nameError — not snackbar`() = runTest {
        // Seed an active category with the same name as we're about to create
        coEvery { dao.getActive() } returns listOf(entity("x", "Hobbies", isDefault = false, archived = false))

        advanceUntilIdle()
        viewModel.onEvent(ManageCategoriesEvent.ShowCreateDialog)
        viewModel.onEvent(ManageCategoriesEvent.FormNameChanged("hobbies"))  // case-insensitive duplicate
        viewModel.onEvent(ManageCategoriesEvent.SaveForm)
        advanceUntilIdle()

        val form = viewModel.uiState.value.form
        assertNotNull("Dialog must stay open on duplicate name error", form)
        assertNotNull("nameError must be set", form!!.nameError)
        assertTrue(form.nameError!!.contains("already exists"))
        // snackbar must NOT be shown for validation errors
        assertNull(viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun `SaveForm create with empty name surfaces nameError`() = runTest {
        coEvery { dao.getActive() } returns emptyList()

        advanceUntilIdle()
        viewModel.onEvent(ManageCategoriesEvent.ShowCreateDialog)
        // Name left empty (default "")
        viewModel.onEvent(ManageCategoriesEvent.SaveForm)
        advanceUntilIdle()

        val form = viewModel.uiState.value.form
        assertNotNull("Dialog must stay open on empty-name error", form)
        assertNotNull("nameError must be set", form!!.nameError)
        assertTrue(form.nameError!!.contains("empty"))
        assertNull(viewModel.uiState.value.snackbarMessage)
    }

    // ── Edit ──────────────────────────────────────────────────────────────────

    @Test
    fun `ShowEditDialog pre-fills form with category data and sets editingKey`() = runTest {
        advanceUntilIdle()
        val cat = domainCategory("custom-1", "Hobbies", isDefault = false, archived = false)
        viewModel.onEvent(ManageCategoriesEvent.ShowEditDialog(cat))

        val form = viewModel.uiState.value.form
        assertNotNull(form)
        assertEquals("custom-1", form!!.editingKey)
        assertEquals("Hobbies", form.name)
        assertEquals(cat.icon, form.icon)
    }

    @Test
    fun `SaveForm edit calls UpdateCategoryUseCase and closes dialog`() = runTest {
        // Provide existing entity so repo can preserve remoteId
        coEvery { dao.getByKey("custom-1") } returns entityCustomActive

        advanceUntilIdle()
        val cat = domainCategory("custom-1", "Hobbies", isDefault = false, archived = false)
        viewModel.onEvent(ManageCategoriesEvent.ShowEditDialog(cat))
        viewModel.onEvent(ManageCategoriesEvent.FormNameChanged("Hobbies Updated"))
        viewModel.onEvent(ManageCategoriesEvent.SaveForm)
        advanceUntilIdle()

        assertNull("Form should close after successful edit", viewModel.uiState.value.form)
        assertNull(viewModel.uiState.value.snackbarMessage)
        coVerify(exactly = 1) { dao.upsert(match { it.key == "custom-1" && it.name == "Hobbies Updated" }) }
    }

    // ── Archive / Unarchive ───────────────────────────────────────────────────

    @Test
    fun `Archive sets archived=true on the stored entity`() = runTest {
        coEvery { dao.getByKey("custom-1") } returns entityCustomActive

        advanceUntilIdle()
        viewModel.onEvent(ManageCategoriesEvent.Archive("custom-1"))
        advanceUntilIdle()

        coVerify { dao.upsert(match { it.archived && it.key == "custom-1" }) }
    }

    @Test
    fun `Unarchive sets archived=false on the stored entity`() = runTest {
        coEvery { dao.getByKey("custom-2") } returns entityCustomArchived

        advanceUntilIdle()
        viewModel.onEvent(ManageCategoriesEvent.Unarchive("custom-2"))
        advanceUntilIdle()

        coVerify { dao.upsert(match { !it.archived && it.key == "custom-2" }) }
    }

    @Test
    fun `Archive failure surfaces snackbar message`() = runTest {
        coEvery { dao.getByKey("custom-1") } returns null  // entity not found → NoSuchElementException

        advanceUntilIdle()
        viewModel.onEvent(ManageCategoriesEvent.Archive("custom-1"))
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.snackbarMessage)
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Test
    fun `ShowDeleteConfirm sets pendingDeleteKey`() = runTest {
        advanceUntilIdle()
        viewModel.onEvent(ManageCategoriesEvent.ShowDeleteConfirm("custom-1"))

        assertEquals("custom-1", viewModel.uiState.value.pendingDeleteKey)
    }

    @Test
    fun `ConfirmDelete custom with 0 usage calls dao deleteByKey`() = runTest {
        coEvery { dao.getByKey("custom-1") } returns entityCustomActive
        coEvery { dao.countTransactionsUsing("custom-1") } returns 0

        advanceUntilIdle()
        viewModel.onEvent(ManageCategoriesEvent.ShowDeleteConfirm("custom-1"))
        viewModel.onEvent(ManageCategoriesEvent.ConfirmDelete)
        advanceUntilIdle()

        coVerify(exactly = 1) { dao.deleteByKey("custom-1") }
        assertNull(viewModel.uiState.value.pendingDeleteKey)
    }

    @Test
    fun `Delete blocked in-use surfaces snackbar with archive suggestion`() = runTest {
        coEvery { dao.getByKey("custom-1") } returns entityCustomActive
        coEvery { dao.countTransactionsUsing("custom-1") } returns 3

        advanceUntilIdle()
        viewModel.onEvent(ManageCategoriesEvent.ShowDeleteConfirm("custom-1"))
        viewModel.onEvent(ManageCategoriesEvent.ConfirmDelete)
        advanceUntilIdle()

        val msg = viewModel.uiState.value.snackbarMessage
        assertNotNull(msg)
        assertTrue("Message should mention archive", msg!!.contains("archive", ignoreCase = true))
        coVerify(exactly = 0) { dao.deleteByKey(any()) }
    }

    @Test
    fun `Delete blocked for default surfaces snackbar`() = runTest {
        coEvery { dao.getByKey("FOOD") } returns entityFood

        advanceUntilIdle()
        viewModel.onEvent(ManageCategoriesEvent.ShowDeleteConfirm("FOOD"))
        viewModel.onEvent(ManageCategoriesEvent.ConfirmDelete)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.snackbarMessage)
        coVerify(exactly = 0) { dao.deleteByKey(any()) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun entity(
        key: String, name: String,
        isDefault: Boolean, archived: Boolean,
        remoteId: String? = null
    ) = CategoryEntity(
        key = key, name = name, icon = "📌",
        colorArgb = 0xFF78909C.toInt(), lightColorArgb = 0xFFECEFF1.toInt(),
        isDefault = isDefault, position = 0,
        archived = archived, remoteId = remoteId,
        synced = false, updatedAt = 0L
    )

    private fun domainCategory(
        key: String, name: String,
        isDefault: Boolean, archived: Boolean
    ) = Category(
        key = key, name = name, icon = "📌",
        color = Color(0xFF78909C), lightColor = Color(0xFFECEFF1),
        isDefault = isDefault, position = 0, archived = archived
    )
}
