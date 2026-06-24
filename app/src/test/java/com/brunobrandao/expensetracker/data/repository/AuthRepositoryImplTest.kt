package com.brunobrandao.expensetracker.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryImplTest {

    private val auth = mockk<FirebaseAuth>(relaxed = true)
    private val user = mockk<FirebaseUser>(relaxed = true)
    private val fakeCredential = mockk<AuthCredential>(relaxed = true)

    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setup() {
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        mockkStatic(EmailAuthProvider::class)
        every { auth.currentUser } returns user
        every { user.email } returns "test@example.com"
        every { EmailAuthProvider.getCredential(any(), any()) } returns fakeCredential
        repository = AuthRepositoryImpl(auth)
    }

    @After
    fun tearDown() {
        unmockkStatic("kotlinx.coroutines.tasks.TasksKt")
        unmockkStatic(EmailAuthProvider::class)
    }

    // ── currentUserEmail ──────────────────────────────────────────────────────

    @Test
    fun `currentUserEmail returns email from current user`() {
        every { user.email } returns "hello@example.com"
        assertTrue(repository.currentUserEmail == "hello@example.com")
    }

    @Test
    fun `currentUserEmail returns null when no user logged in`() {
        every { auth.currentUser } returns null
        assertNull(repository.currentUserEmail)
    }

    // ── reauthenticate ────────────────────────────────────────────────────────

    @Test
    fun `reauthenticate returns success when Firebase reauthentication succeeds`() = runTest {
        val task = mockk<Task<Void>> {
            every { isComplete } returns true
            every { isSuccessful } returns true
            every { isCanceled } returns false
            every { result } returns null
            every { exception } returns null
        }
        every { user.reauthenticate(any()) } returns task

        val result = repository.reauthenticate("correct-password")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `reauthenticate returns failure when Firebase throws invalid credentials`() = runTest {
        val exception = mockk<FirebaseAuthInvalidCredentialsException>(relaxed = true)
        every { user.reauthenticate(any()) } throws exception

        val result = repository.reauthenticate("wrong-password")

        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun `reauthenticate returns failure when no user is logged in`() = runTest {
        every { auth.currentUser } returns null

        val result = repository.reauthenticate("any-password")

        assertTrue(result.isFailure)
    }

    // ── deleteAccount ─────────────────────────────────────────────────────────

    @Test
    fun `deleteAccount calls user delete and returns success`() = runTest {
        val deleteTask = mockk<Task<Void>> {
            every { isComplete } returns true
            every { isSuccessful } returns true
            every { isCanceled } returns false
            every { result } returns null
            every { exception } returns null
        }
        every { user.delete() } returns deleteTask

        val result = repository.deleteAccount()

        assertTrue(result.isSuccess)
        coVerify { user.delete() }
    }

    @Test
    fun `deleteAccount returns failure when no user is logged in`() = runTest {
        every { auth.currentUser } returns null

        val result = repository.deleteAccount()

        assertTrue(result.isFailure)
    }

    @Test
    fun `deleteAccount returns failure when Firebase delete throws`() = runTest {
        every { user.delete() } throws Exception("Firebase error")

        val result = repository.deleteAccount()

        assertTrue(result.isFailure)
    }
}
