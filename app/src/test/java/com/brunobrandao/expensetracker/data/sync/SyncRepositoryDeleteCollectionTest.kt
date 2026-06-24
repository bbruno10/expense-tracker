package com.brunobrandao.expensetracker.data.sync

import com.brunobrandao.expensetracker.data.local.dao.CategoryDao
import com.brunobrandao.expensetracker.data.local.dao.RecurringTransactionDao
import com.brunobrandao.expensetracker.data.local.dao.TransactionDao
import com.brunobrandao.expensetracker.data.preferences.UserPreferencesRepository
import com.brunobrandao.expensetracker.domain.repository.AuthRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.WriteBatch
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class SyncRepositoryDeleteCollectionTest {

    private val firestore = mockk<FirebaseFirestore>(relaxed = true)
    private val transactionDao = mockk<TransactionDao>(relaxed = true)
    private val recurringDao = mockk<RecurringTransactionDao>(relaxed = true)
    private val categoryDao = mockk<CategoryDao>(relaxed = true)
    private val preferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
    private val authRepository = mockk<AuthRepository>(relaxed = true)

    // Firestore chain for the user doc
    private val usersCol = mockk<CollectionReference>(relaxed = true)
    private val userDoc = mockk<DocumentReference>(relaxed = true)

    private val writeBatch = mockk<WriteBatch>(relaxed = true)

    private lateinit var sut: SyncRepository

    @Before
    fun setup() {
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")

        every { firestore.collection("users") } returns usersCol
        every { usersCol.document("user-1") } returns userDoc
        every { firestore.batch() } returns writeBatch

        // batch.commit() returns a completed Task<Void>
        val commitTask = completedVoidTask()
        every { writeBatch.commit() } returns commitTask

        sut = SyncRepository(
            firestore, authRepository, transactionDao, recurringDao, categoryDao, preferencesRepository
        )
    }

    @After
    fun tearDown() {
        unmockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    // ── Chunking: 450 docs → 2 batch commits ─────────────────────────────────

    @Test
    fun `deleteCollection with 450 docs commits exactly 2 batches`() = runTest {
        val col = mockk<CollectionReference>(relaxed = true)
        every { userDoc.collection("transactions") } returns col

        val docs = (1..450).map { i ->
            mockk<com.google.firebase.firestore.DocumentSnapshot>(relaxed = true) {
                every { reference } returns mockk(relaxed = true)
                every { id } returns "doc-$i"
            }
        }
        val querySnapshot = mockk<QuerySnapshot>(relaxed = true) {
            every { documents } returns docs
        }
        val getTask = mockk<Task<QuerySnapshot>> {
            every { isComplete } returns true
            every { isSuccessful } returns true
            every { isCanceled } returns false
            every { result } returns querySnapshot
            every { exception } returns null
        }
        every { col.get() } returns getTask

        // Also stub settings delete for deleteAllUserDataFromFirestore
        every { userDoc.collection("recurring_transactions") } returns mockk(relaxed = true) {
            every { get() } returns emptyQueryTask()
        }
        every { userDoc.collection("categories") } returns mockk(relaxed = true) {
            every { get() } returns emptyQueryTask()
        }
        val settingsCol = mockk<CollectionReference>(relaxed = true)
        val prefsDoc = mockk<DocumentReference>(relaxed = true)
        every { userDoc.collection("settings") } returns settingsCol
        every { settingsCol.document("preferences") } returns prefsDoc
        every { prefsDoc.delete() } returns completedVoidTask()

        sut.deleteAllUserDataFromFirestore("user-1")

        // 450 docs → chunk(400) = [400, 50] → 2 commits on "transactions" collection
        coVerify(exactly = 2) { writeBatch.commit() }
    }

    // ── Empty collection → 0 commits ─────────────────────────────────────────

    @Test
    fun `deleteCollection with 0 docs does not call batch commit`() = runTest {
        every { userDoc.collection("transactions") } returns mockk(relaxed = true) {
            every { get() } returns emptyQueryTask()
        }
        every { userDoc.collection("recurring_transactions") } returns mockk(relaxed = true) {
            every { get() } returns emptyQueryTask()
        }
        every { userDoc.collection("categories") } returns mockk(relaxed = true) {
            every { get() } returns emptyQueryTask()
        }
        val settingsCol = mockk<CollectionReference>(relaxed = true)
        val prefsDoc = mockk<DocumentReference>(relaxed = true)
        every { userDoc.collection("settings") } returns settingsCol
        every { settingsCol.document("preferences") } returns prefsDoc
        every { prefsDoc.delete() } returns completedVoidTask()

        sut.deleteAllUserDataFromFirestore("user-1")

        coVerify(exactly = 0) { writeBatch.commit() }
    }

    // ── Exactly 400 docs → 1 commit ──────────────────────────────────────────

    @Test
    fun `deleteCollection with exactly 400 docs commits exactly 1 batch`() = runTest {
        val col = mockk<CollectionReference>(relaxed = true)
        every { userDoc.collection("transactions") } returns col

        val docs = (1..400).map { i ->
            mockk<com.google.firebase.firestore.DocumentSnapshot>(relaxed = true) {
                every { reference } returns mockk(relaxed = true)
                every { id } returns "doc-$i"
            }
        }
        val querySnapshot = mockk<QuerySnapshot>(relaxed = true) {
            every { documents } returns docs
        }
        every { col.get() } returns mockk<Task<QuerySnapshot>> {
            every { isComplete } returns true
            every { isSuccessful } returns true
            every { isCanceled } returns false
            every { result } returns querySnapshot
            every { exception } returns null
        }
        every { userDoc.collection("recurring_transactions") } returns mockk(relaxed = true) {
            every { get() } returns emptyQueryTask()
        }
        every { userDoc.collection("categories") } returns mockk(relaxed = true) {
            every { get() } returns emptyQueryTask()
        }
        val settingsCol = mockk<CollectionReference>(relaxed = true)
        val prefsDoc = mockk<DocumentReference>(relaxed = true)
        every { userDoc.collection("settings") } returns settingsCol
        every { settingsCol.document("preferences") } returns prefsDoc
        every { prefsDoc.delete() } returns completedVoidTask()

        sut.deleteAllUserDataFromFirestore("user-1")

        coVerify(exactly = 1) { writeBatch.commit() }
    }

    // ── deleteAllLocalData ────────────────────────────────────────────────────

    @Test
    fun `deleteAllLocalData clears all three Room tables`() = runTest {
        sut.deleteAllLocalData()

        coVerify(exactly = 1) { transactionDao.deleteAll() }
        coVerify(exactly = 1) { recurringDao.deleteAll() }
        coVerify(exactly = 1) { categoryDao.deleteAll() }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun completedVoidTask(): Task<Void> = mockk {
        every { isComplete } returns true
        every { isSuccessful } returns true
        every { isCanceled } returns false
        every { result } returns null
        every { exception } returns null
    }

    private fun emptyQueryTask(): Task<QuerySnapshot> = mockk {
        every { isComplete } returns true
        every { isSuccessful } returns true
        every { isCanceled } returns false
        every { result } returns mockk(relaxed = true) { every { documents } returns emptyList() }
        every { exception } returns null
    }
}
