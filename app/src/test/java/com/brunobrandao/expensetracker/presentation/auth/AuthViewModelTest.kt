package com.brunobrandao.expensetracker.presentation.auth

import com.brunobrandao.expensetracker.data.sync.SyncRepository
import com.brunobrandao.expensetracker.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var authRepository: AuthRepository
    private lateinit var syncRepository: SyncRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
        syncRepository = mockk(relaxed = true)
        every { authRepository.currentUserId } returns "user-1"
        viewModel = AuthViewModel(authRepository, syncRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `signOut stops listeners and delegates cleanup to SyncRepository`() = runTest {
        viewModel.signOut()

        verify(exactly = 1) { syncRepository.stopSync() }
        verify(exactly = 1) { syncRepository.signOutAndCleanup("user-1") }
    }

    @Test
    fun `signOut calls authRepository signOut directly when no session`() = runTest {
        every { authRepository.currentUserId } returns null

        viewModel.signOut()

        verify(exactly = 1) { syncRepository.stopSync() }
        verify(exactly = 0) { syncRepository.signOutAndCleanup(any()) }
        verify(exactly = 1) { authRepository.signOut() }
    }

    // ── sendPasswordReset ─────────────────────────────────────────────────────

    @Test
    fun `sendPasswordReset with invalid email sets errorMessage and does not call repository`() = runTest {
        viewModel.onEmailChange("not-an-email")

        viewModel.sendPasswordReset()
        advanceUntilIdle()

        assertEquals("Invalid email address", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.infoMessage)
        coVerify(exactly = 0) { authRepository.sendPasswordReset(any()) }
    }

    @Test
    fun `sendPasswordReset with valid email on success sets infoMessage`() = runTest {
        viewModel.onEmailChange("user@example.com")
        coEvery { authRepository.sendPasswordReset("user@example.com") } returns Result.success(Unit)

        viewModel.sendPasswordReset()
        advanceUntilIdle()

        assertEquals(
            "If an account exists for this email, a reset link has been sent.",
            viewModel.uiState.value.infoMessage
        )
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `sendPasswordReset with FirebaseAuthInvalidUserException shows same generic infoMessage`() = runTest {
        viewModel.onEmailChange("unknown@example.com")
        coEvery { authRepository.sendPasswordReset("unknown@example.com") } returns
            Result.failure(mockk<FirebaseAuthInvalidUserException>(relaxed = true))

        viewModel.sendPasswordReset()
        advanceUntilIdle()

        assertEquals(
            "If an account exists for this email, a reset link has been sent.",
            viewModel.uiState.value.infoMessage
        )
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `sendPasswordReset on network error sets errorMessage`() = runTest {
        viewModel.onEmailChange("user@example.com")
        coEvery { authRepository.sendPasswordReset("user@example.com") } returns
            Result.failure(RuntimeException("Network error"))

        viewModel.sendPasswordReset()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.infoMessage)
    }
}
