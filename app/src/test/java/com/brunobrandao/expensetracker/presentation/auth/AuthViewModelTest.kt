package com.brunobrandao.expensetracker.presentation.auth

import com.brunobrandao.expensetracker.data.preferences.UserPreferencesRepository
import com.brunobrandao.expensetracker.data.sync.SyncRepository
import com.brunobrandao.expensetracker.domain.repository.AuthRepository
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var authRepository: AuthRepository
    private lateinit var syncRepository: SyncRepository
    private lateinit var preferencesRepository: UserPreferencesRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
        syncRepository = mockk(relaxed = true)
        preferencesRepository = mockk(relaxed = true)
        every { authRepository.currentUserId } returns "user-1"
        viewModel = AuthViewModel(authRepository, syncRepository, preferencesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `signOut clears currency AFTER signing out of auth`() = runTest {
        viewModel.signOut()
        advanceUntilIdle()

        coVerifyOrder {
            authRepository.signOut()
            preferencesRepository.clearCurrency()
        }
    }

    @Test
    fun `signOut clears currency even when userId is null`() = runTest {
        every { authRepository.currentUserId } returns null

        viewModel.signOut()
        advanceUntilIdle()

        coVerifyOrder {
            authRepository.signOut()
            preferencesRepository.clearCurrency()
        }
    }
}
