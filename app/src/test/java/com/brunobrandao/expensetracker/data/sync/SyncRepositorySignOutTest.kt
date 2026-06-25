package com.brunobrandao.expensetracker.data.sync

import com.brunobrandao.expensetracker.data.local.dao.CategoryDao
import com.brunobrandao.expensetracker.data.local.dao.RecurringTransactionDao
import com.brunobrandao.expensetracker.data.local.dao.TransactionDao
import com.brunobrandao.expensetracker.data.preferences.UserPreferencesRepository
import com.brunobrandao.expensetracker.domain.repository.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class SyncRepositorySignOutTest {

    private val categoryDao = mockk<CategoryDao>(relaxed = true)
    private val transactionDao = mockk<TransactionDao>(relaxed = true)
    private val recurringDao = mockk<RecurringTransactionDao>(relaxed = true)
    private val preferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val firestore = mockk<FirebaseFirestore>(relaxed = true)

    private lateinit var sut: SyncRepository

    @Before
    fun setup() {
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        coEvery { categoryDao.getUnsynced() } returns emptyList()
        coEvery { transactionDao.getUnsyncedTransactions() } returns emptyList()
        coEvery { recurringDao.getUnsyncedTransactions() } returns emptyList()

        sut = SyncRepository(
            firestore, authRepository, transactionDao, recurringDao,
            categoryDao, preferencesRepository
        )
    }

    @After
    fun tearDown() {
        unmockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    // ── Test 1: central regression ────────────────────────────────────────────
    // signOutAndCleanup() is NOT suspend — it returns immediately after submitting
    // work to SyncRepository's @Singleton scope. The caller (ViewModel) can be
    // destroyed at any point after the call; the cleanup continues independently.

    @Test
    fun `signOutAndCleanup is fire-and-forget and completes after caller returns`() = runTest {
        // signOutAndCleanup() returns synchronously after launching cleanupJob.
        // In production, the ViewModel scope is cancelled by navigation immediately
        // after this call — that cancellation cannot reach cleanupJob because it
        // lives in SyncRepository.scope, not in the ViewModel's scope.
        sut.signOutAndCleanup("user-a")

        // Wait for the independent cleanup job to finish.
        sut.cleanupJob!!.join()

        coVerify(exactly = 1) { categoryDao.deleteCustomCategories() }
        coVerify(exactly = 1) { preferencesRepository.clearCurrency() }
        verify(exactly = 1) { authRepository.signOut() }
    }

    @Test
    fun `signOutAndCleanup cleanup is not interrupted by cancellation of an external scope`() = runTest {
        // Create a scope that mimics the ViewModel's viewModelScope.
        val simulatedViewModelScope = CoroutineScope(SupervisorJob())

        // The ViewModel calls signOutAndCleanup from its scope and immediately
        // has its scope cancelled (navigation with popUpTo(0)).
        simulatedViewModelScope.launch { sut.signOutAndCleanup("user-a") }.join()
        simulatedViewModelScope.cancel()

        // cleanupJob is in SyncRepository.scope — unaffected by the VM scope cancel.
        sut.cleanupJob!!.join()

        coVerify(exactly = 1) { categoryDao.deleteCustomCategories() }
        coVerify(exactly = 1) { preferencesRepository.clearCurrency() }
        verify(exactly = 1) { authRepository.signOut() }
    }

    // ── Test 2: no-guard on categories ───────────────────────────────────────
    // A failure during push (e.g. Room error on getUnsynced) must not skip the
    // deleteCustomCategories step. Financial data guard (transactions) is unaffected.

    @Test
    fun `signOutAndCleanup deletes custom categories even when push throws`() = runTest {
        coEvery { categoryDao.getUnsynced() } throws RuntimeException("DB error")

        sut.signOutAndCleanup("user-a")
        sut.cleanupJob!!.join()

        coVerify(exactly = 1) { categoryDao.deleteCustomCategories() }
        verify(exactly = 1) { authRepository.signOut() }
    }

    // ── Test 3: ordering guarantee ────────────────────────────────────────────
    // The full sequence inside signOutAndCleanup must be:
    //   deleteCustomCategories (inside pushPendingAndClear) → clearCurrency → signOut

    @Test
    fun `signOutAndCleanup executes deleteCustom then clearCurrency then signOut in order`() = runTest {
        sut.signOutAndCleanup("user-a")
        sut.cleanupJob!!.join()

        coVerifyOrder {
            categoryDao.deleteCustomCategories()
            preferencesRepository.clearCurrency()
            authRepository.signOut()
        }
    }

    // ── Test 4: serialization against the next login ─────────────────────────
    // startSync() must not attach the category listener (or begin pulling currency
    // for the new user) until the previous session's cleanupJob is done.
    // We block clearCurrency() with a barrier so the cleanup stalls mid-flight,
    // then call startSync() and verify pullCurrency("user-b") only runs after
    // clearCurrency() returns.

    @Test
    fun `startSync does not start new session until cleanupJob completes`() = runTest {
        val barrier = CompletableDeferred<Unit>()
        coEvery { preferencesRepository.clearCurrency() } coAnswers { barrier.await() }

        // Start cleanup for user-a (stalls at clearCurrency)
        sut.signOutAndCleanup("user-a")

        // Immediately request sync for user-b; syncJob will block on cleanupJob?.join()
        sut.startSync("user-b")

        // Release the barrier — cleanup finishes, then startSync proceeds
        barrier.complete(Unit)

        sut.cleanupJob?.join()
        sut.syncJob?.join()

        // pullCurrency for user-b (the first action inside startSync's coroutine)
        // must execute after clearCurrency (the blocked step in cleanup)
        coVerifyOrder {
            preferencesRepository.clearCurrency()          // from signOutAndCleanup
            preferencesRepository.pullCurrency("user-b")  // from startSync
        }
    }

    // ── Test 5: defaults survive cleanup ─────────────────────────────────────
    // deleteCustomCategories uses WHERE isDefault=0; the DAO-level contract is
    // tested here by verifying deleteAll() is never called during signOutAndCleanup.

    @Test
    fun `signOutAndCleanup never calls deleteAll on categoryDao`() = runTest {
        sut.signOutAndCleanup("user-a")
        sut.cleanupJob!!.join()

        coVerify(exactly = 0) { categoryDao.deleteAll() }
        coVerify(exactly = 1) { categoryDao.deleteCustomCategories() }
    }
}
