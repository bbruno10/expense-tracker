package com.brunobrandao.expensetracker.data.sync

import com.brunobrandao.expensetracker.data.local.dao.CategoryDao
import com.brunobrandao.expensetracker.data.local.dao.RecurringTransactionDao
import com.brunobrandao.expensetracker.data.local.dao.TransactionDao
import com.brunobrandao.expensetracker.data.local.entity.CategoryEntity
import com.brunobrandao.expensetracker.data.preferences.UserPreferencesRepository
import com.brunobrandao.expensetracker.domain.repository.AuthRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CategorySyncTest {

    private val categoryDao = mockk<CategoryDao>(relaxed = true)
    private val transactionDao = mockk<TransactionDao>(relaxed = true)
    private val recurringDao = mockk<RecurringTransactionDao>(relaxed = true)
    private val preferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val firestore = mockk<FirebaseFirestore>(relaxed = true)

    private lateinit var sut: SyncRepository

    @Before
    fun setup() {
        // Wire Firestore chain for categories; set() returns a completed Task<Void>.
        val completedTask = mockk<Task<Void>> {
            every { isComplete } returns true
            every { isSuccessful } returns true
            every { isCanceled } returns false
            every { result } returns null
            every { exception } returns null
        }
        val mockDocRef = mockk<DocumentReference>(relaxed = true) {
            every { set(any<Map<String, Any>>()) } returns completedTask
        }
        val mockCategoryCol = mockk<CollectionReference>(relaxed = true) {
            every { document(any()) } returns mockDocRef
        }
        val mockUserDoc = mockk<DocumentReference>(relaxed = true) {
            every { collection("categories") } returns mockCategoryCol
        }
        val mockUsersCol = mockk<CollectionReference>(relaxed = true) {
            every { document(any()) } returns mockUserDoc
        }
        every { firestore.collection("users") } returns mockUsersCol

        sut = SyncRepository(
            firestore, authRepository, transactionDao, recurringDao, categoryDao, preferencesRepository
        )
    }

    // ── Push ──────────────────────────────────────────────────────────────────

    @Test
    fun `push unsynced with null remoteId generates UUID and marks synced`() = runTest {
        val entity = entity(remoteId = null, synced = false)
        coEvery { categoryDao.getUnsynced() } returns listOf(entity)
        val upsertSlot = slot<CategoryEntity>()
        coEvery { categoryDao.upsert(capture(upsertSlot)) } just Runs

        sut.pushUnsyncedCategories("user123")

        val saved = upsertSlot.captured
        assertNotNull(saved.remoteId)
        assertEquals(36, saved.remoteId!!.length)
        assertTrue(saved.remoteId!!.contains("-"))
        coVerify(exactly = 1) { categoryDao.markSynced(entity.key) }
    }

    @Test
    fun `push unsynced with existing remoteId does not generate a new UUID`() = runTest {
        val entity = entity(remoteId = "existing-remote-id", synced = false)
        coEvery { categoryDao.getUnsynced() } returns listOf(entity)

        sut.pushUnsyncedCategories("user123")

        coVerify(exactly = 0) { categoryDao.upsert(any()) }
        coVerify(exactly = 1) { categoryDao.markSynced(entity.key) }
    }

    @Test
    fun `push skips when there are no unsynced categories`() = runTest {
        coEvery { categoryDao.getUnsynced() } returns emptyList()

        sut.pushUnsyncedCategories("user123")

        coVerify(exactly = 0) { categoryDao.upsert(any()) }
        coVerify(exactly = 0) { categoryDao.markSynced(any()) }
    }

    // ── Pull ─────────────────────────────────────────────────────────────────

    @Test
    fun `pull ADDED with smaller remote updatedAt does not overwrite local`() = runTest {
        val snapshot = mockSnapshot(docId = "remote1", key = "cat-key", updatedAt = 500L)
        val existing = entity(key = "cat-key", remoteId = "remote1", synced = true, updatedAt = 1000L)
        coEvery { categoryDao.getByRemoteId("remote1") } returns existing

        sut.processCategoryChange(DocumentChange.Type.ADDED, snapshot)

        coVerify(exactly = 0) { categoryDao.upsert(any()) }
    }

    @Test
    fun `pull MODIFIED with larger remote updatedAt overwrites local`() = runTest {
        val snapshot = mockSnapshot(docId = "remote1", key = "cat-key", updatedAt = 2000L)
        val existing = entity(key = "cat-key", remoteId = "remote1", synced = true, updatedAt = 1000L)
        coEvery { categoryDao.getByRemoteId("remote1") } returns existing
        val upsertSlot = slot<CategoryEntity>()
        coEvery { categoryDao.upsert(capture(upsertSlot)) } just Runs

        sut.processCategoryChange(DocumentChange.Type.MODIFIED, snapshot)

        coVerify(exactly = 1) { categoryDao.upsert(any()) }
        assertEquals("cat-key", upsertSlot.captured.key)
        assertTrue(upsertSlot.captured.synced)
    }

    @Test
    fun `pull MODIFIED with unsynced local entry is overwritten regardless of updatedAt`() = runTest {
        val snapshot = mockSnapshot(docId = "remote1", key = "cat-key", updatedAt = 500L)
        val existing = entity(key = "cat-key", remoteId = "remote1", synced = false, updatedAt = 1000L)
        coEvery { categoryDao.getByRemoteId("remote1") } returns existing
        val upsertSlot = slot<CategoryEntity>()
        coEvery { categoryDao.upsert(capture(upsertSlot)) } just Runs

        sut.processCategoryChange(DocumentChange.Type.MODIFIED, snapshot)

        coVerify(exactly = 1) { categoryDao.upsert(any()) }
    }

    @Test
    fun `pull MODIFIED with archived=true propagates soft-delete to local`() = runTest {
        val snapshot = mockSnapshot(docId = "remote1", key = "cat-key", archived = true, updatedAt = 2000L)
        val existing = entity(key = "cat-key", remoteId = "remote1", synced = true, updatedAt = 1000L)
        coEvery { categoryDao.getByRemoteId("remote1") } returns existing
        val upsertSlot = slot<CategoryEntity>()
        coEvery { categoryDao.upsert(capture(upsertSlot)) } just Runs

        sut.processCategoryChange(DocumentChange.Type.MODIFIED, snapshot)

        val saved = upsertSlot.captured
        assertTrue(saved.archived)
        assertEquals("cat-key", saved.key)
    }

    @Test
    fun `pull ADDED for unknown remoteId inserts as new local category`() = runTest {
        val snapshot = mockSnapshot(docId = "remote-new", key = "brand-new-key", updatedAt = 1000L)
        coEvery { categoryDao.getByRemoteId("remote-new") } returns null
        val upsertSlot = slot<CategoryEntity>()
        coEvery { categoryDao.upsert(capture(upsertSlot)) } just Runs

        sut.processCategoryChange(DocumentChange.Type.ADDED, snapshot)

        coVerify(exactly = 1) { categoryDao.upsert(any()) }
        assertEquals("brand-new-key", upsertSlot.captured.key)
        assertEquals("remote-new", upsertSlot.captured.remoteId)
    }

    @Test
    fun `pull REMOVED is ignored and does not alter local state`() = runTest {
        val snapshot = mockSnapshot(docId = "remote1", key = "cat-key")

        sut.processCategoryChange(DocumentChange.Type.REMOVED, snapshot)

        coVerify(exactly = 0) { categoryDao.upsert(any()) }
        coVerify(exactly = 0) { categoryDao.deleteByKey(any()) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun entity(
        key: String = "test-key",
        remoteId: String? = null,
        synced: Boolean = false,
        updatedAt: Long = 0L,
        archived: Boolean = false
    ) = CategoryEntity(
        key = key,
        name = "Test",
        icon = "📌",
        colorArgb = 0xFF78909C.toInt(),
        lightColorArgb = 0xFFECEFF1.toInt(),
        isDefault = false,
        position = 0,
        archived = archived,
        remoteId = remoteId,
        synced = synced,
        updatedAt = updatedAt
    )

    private fun mockSnapshot(
        docId: String,
        key: String,
        archived: Boolean = false,
        updatedAt: Long = 1000L
    ): DocumentSnapshot = mockk<DocumentSnapshot>(relaxed = true) {
        every { id } returns docId
        every { getString("key") } returns key
        every { getString("name") } returns "Test"
        every { getString("icon") } returns "📌"
        every { getLong("colorArgb") } returns 0xFF78909CL
        every { getLong("lightColorArgb") } returns 0xFFECEFF1L
        every { getBoolean("isDefault") } returns false
        every { getLong("position") } returns 0L
        every { getBoolean("archived") } returns archived
        every { getLong("updatedAt") } returns updatedAt
    }
}
