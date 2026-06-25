package com.brunobrandao.expensetracker.presentation.auth

import com.brunobrandao.expensetracker.data.sync.SyncRepository
import com.brunobrandao.expensetracker.domain.repository.AuthRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
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
}
