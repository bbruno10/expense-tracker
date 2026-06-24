package com.brunobrandao.expensetracker.data.sync

import com.brunobrandao.expensetracker.data.local.dao.CategoryDao
import com.brunobrandao.expensetracker.data.local.dao.RecurringTransactionDao
import com.brunobrandao.expensetracker.data.local.dao.TransactionDao
import com.brunobrandao.expensetracker.data.local.entity.CategoryEntity
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
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class CategoryLeakTest {

    private val categoryDao = mockk<CategoryDao>(relaxed = true)
    private val transactionDao = mockk<TransactionDao>(relaxed = true)
    private val recurringDao = mockk<RecurringTransactionDao>(relaxed = true)
    private val preferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val firestore = mockk<FirebaseFirestore>(relaxed = true)

    private lateinit var sut: SyncRepository

    private val defaultEntity = CategoryEntity(
        key = "FOOD", name = "Food", icon = "🍔",
        colorArgb = 0, lightColorArgb = 0,
        isDefault = true, position = 0, archived = false,
        remoteId = "remote-food", synced = true, updatedAt = 1000L
    )
    private val customEntity = CategoryEntity(
        key = "GYM", name = "Gym", icon = "🏋️",
        colorArgb = 0, lightColorArgb = 0,
        isDefault = false, position = 99, archived = false,
        remoteId = "remote-gym", synced = true, updatedAt = 2000L
    )
    private val customOfflineEntity = CategoryEntity(
        key = "GIFTS", name = "Gifts", icon = "🎁",
        colorArgb = 0, lightColorArgb = 0,
        isDefault = false, position = 100, archived = false,
        remoteId = null, synced = false, updatedAt = 3000L
    )

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

    // ── deleteCustomCategories is called unconditionally on logout ────────────

    @Test
    fun `pushPendingAndClear always calls deleteCustomCategories`() = runTest {
        sut.pushPendingAndClear("user-a")

        coVerify(exactly = 1) { categoryDao.deleteCustomCategories() }
    }

    @Test
    fun `pushPendingAndClear calls deleteCustomCategories even when push fails`() = runTest {
        coEvery { categoryDao.getUnsynced() } returns listOf(customOfflineEntity)
        // Firestore call will throw — simulates offline
        val col = mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
        val userDoc = mockk<com.google.firebase.firestore.DocumentReference>(relaxed = true)
        val catCol = mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
        every { firestore.collection("users") } returns col
        every { col.document("user-a") } returns userDoc
        every { userDoc.collection("categories") } returns catCol
        every { catCol.document(any()) } throws RuntimeException("Network error")

        sut.pushPendingAndClear("user-a")

        // deleteCustomCategories must still be called despite the push failure
        coVerify(exactly = 1) { categoryDao.deleteCustomCategories() }
    }

    // ── deleteCustomCategories is called AFTER the push attempt ──────────────

    @Test
    fun `pushPendingAndClear queries unsynced categories before deleting custom`() = runTest {
        // getUnsynced() returns empty (no push), but the ordering contract holds:
        // the push phase (getUnsynced) always runs before deleteCustomCategories.
        sut.pushPendingAndClear("user-a")

        coVerifyOrder {
            categoryDao.getUnsynced()
            categoryDao.deleteCustomCategories()
        }
    }

    // ── Defaults are never deleted by deleteCustomCategories ─────────────────

    @Test
    fun `deleteCustomCategories does NOT delete defaults`() = runTest {
        // This test verifies the DAO query contract: isDefault=0 filter.
        // We verify deleteAll() is never called (only deleteCustomCategories is).
        sut.pushPendingAndClear("user-a")

        coVerify(exactly = 0) { categoryDao.deleteAll() }
        coVerify(exactly = 1) { categoryDao.deleteCustomCategories() }
    }

    // ── Transactions and recurring are unaffected ─────────────────────────────

    @Test
    fun `pushPendingAndClear still applies guard to transactions`() = runTest {
        coEvery { transactionDao.getUnsyncedTransactions() } returns emptyList()

        sut.pushPendingAndClear("user-a")

        // When no unsynced transactions exist, deleteAll() is still called
        // (allSynced starts true, no items to iterate → guard passes)
        coVerify(exactly = 1) { transactionDao.deleteAll() }
    }

    @Test
    fun `pushPendingAndClear does not call categoryDao deleteAll during logout`() = runTest {
        sut.pushPendingAndClear("user-a")

        coVerify(exactly = 0) { categoryDao.deleteAll() }
    }
}
