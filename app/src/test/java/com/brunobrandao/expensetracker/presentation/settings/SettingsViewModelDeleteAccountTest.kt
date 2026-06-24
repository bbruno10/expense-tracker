package com.brunobrandao.expensetracker.presentation.settings

import com.brunobrandao.expensetracker.data.preferences.UserPreferencesRepository
import com.brunobrandao.expensetracker.data.sync.SyncRepository
import com.brunobrandao.expensetracker.domain.repository.AuthRepository
import com.brunobrandao.expensetracker.domain.repository.CategoryRepository
import com.brunobrandao.expensetracker.domain.usecase.GetTransactionsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelDeleteAccountTest {

    private val testDispatcher = StandardTestDispatcher()

    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val syncRepository = mockk<SyncRepository>(relaxed = true)
    private val preferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
    private val getTransactions = mockk<GetTransactionsUseCase>(relaxed = true)
    private val categoryRepository = mockk<CategoryRepository>(relaxed = true)

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { authRepository.currentUserId } returns "user-1"
        viewModel = SettingsViewModel(
            preferencesRepository = preferencesRepository,
            getTransactions = getTransactions,
            categoryRepository = categoryRepository,
            context = mockk(relaxed = true),
            authRepository = authRepository,
            syncRepository = syncRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Wrong password → nothing deleted ─────────────────────────────────────

    @Test
    fun `wrong password shows error and does not touch any data`() = runTest {
        coEvery { authRepository.reauthenticate("wrong") } returns
            Result.failure(Exception("The password is invalid"))

        viewModel.onEvent(SettingsEvent.DeleteAccount("wrong"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isDeleteLoading)
        assertNotNull(state.deleteErrorMessage)

        coVerify(exactly = 0) { syncRepository.deleteAllUserDataFromFirestore(any()) }
        coVerify(exactly = 0) { syncRepository.deleteAllLocalData() }
        coVerify(exactly = 0) { preferencesRepository.clearAllPreferences() }
        coVerify(exactly = 0) { authRepository.deleteAccount() }
    }

    // ── Firestore failure → account intact ───────────────────────────────────

    @Test
    fun `firestore deletion failure shows error and stops before local data and auth delete`() = runTest {
        coEvery { authRepository.reauthenticate(any()) } returns Result.success(Unit)
        coEvery { syncRepository.deleteAllUserDataFromFirestore("user-1") } throws
            Exception("Network unavailable")

        viewModel.onEvent(SettingsEvent.DeleteAccount("correct-password"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isDeleteLoading)
        assertNotNull(state.deleteErrorMessage)

        coVerify(exactly = 0) { syncRepository.deleteAllLocalData() }
        coVerify(exactly = 0) { preferencesRepository.clearAllPreferences() }
        coVerify(exactly = 0) { authRepository.deleteAccount() }
    }

    // ── Success → full sequence in correct order ──────────────────────────────

    @Test
    fun `successful deletion calls all steps in the correct order`() = runTest {
        coEvery { authRepository.reauthenticate(any()) } returns Result.success(Unit)
        coEvery { syncRepository.deleteAllUserDataFromFirestore(any()) } returns Unit
        coEvery { syncRepository.deleteAllLocalData() } returns Unit
        coEvery { preferencesRepository.clearAllPreferences() } returns Unit
        coEvery { authRepository.deleteAccount() } returns Result.success(Unit)

        var successCallbackFired = false
        viewModel.onEvent(SettingsEvent.DeleteAccount("correct-password")) {
            successCallbackFired = true
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isDeleteLoading)
        assertNull(state.deleteErrorMessage)
        assertFalse(state.showDeleteAccountDialog)
        assertTrue(successCallbackFired)

        coVerifyOrder {
            authRepository.reauthenticate("correct-password")
            syncRepository.deleteAllUserDataFromFirestore("user-1")
            syncRepository.deleteAllLocalData()
            preferencesRepository.clearAllPreferences()
            authRepository.deleteAccount()
        }
    }

    // ── Loading state ─────────────────────────────────────────────────────────

    @Test
    fun `isDeleteLoading is true while deletion is in progress`() = runTest {
        coEvery { authRepository.reauthenticate(any()) } returns Result.success(Unit)
        coEvery { syncRepository.deleteAllUserDataFromFirestore(any()) } returns Unit
        coEvery { syncRepository.deleteAllLocalData() } returns Unit
        coEvery { preferencesRepository.clearAllPreferences() } returns Unit
        coEvery { authRepository.deleteAccount() } returns Result.success(Unit)

        viewModel.onEvent(SettingsEvent.DeleteAccount("pw"))

        // Before advanceUntilIdle, the loading state should be set
        assertTrue(viewModel.uiState.value.isDeleteLoading)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDeleteLoading)
    }

    // ── Dialog toggle ─────────────────────────────────────────────────────────

    @Test
    fun `ToggleDeleteAccountDialog opens and closes dialog and clears error`() {
        viewModel.onEvent(SettingsEvent.ToggleDeleteAccountDialog)
        assertTrue(viewModel.uiState.value.showDeleteAccountDialog)

        viewModel.onEvent(SettingsEvent.ToggleDeleteAccountDialog)
        assertFalse(viewModel.uiState.value.showDeleteAccountDialog)
        assertNull(viewModel.uiState.value.deleteErrorMessage)
    }

    // ── Auth delete failure after data is cleared ─────────────────────────────

    @Test
    fun `auth delete failure shows error after data has been cleared`() = runTest {
        coEvery { authRepository.reauthenticate(any()) } returns Result.success(Unit)
        coEvery { syncRepository.deleteAllUserDataFromFirestore(any()) } returns Unit
        coEvery { syncRepository.deleteAllLocalData() } returns Unit
        coEvery { preferencesRepository.clearAllPreferences() } returns Unit
        coEvery { authRepository.deleteAccount() } returns Result.failure(Exception("Delete failed"))

        var successFired = false
        viewModel.onEvent(SettingsEvent.DeleteAccount("pw")) { successFired = true }
        advanceUntilIdle()

        assertFalse(successFired)
        assertFalse(viewModel.uiState.value.isDeleteLoading)
        assertNotNull(viewModel.uiState.value.deleteErrorMessage)
    }
}
