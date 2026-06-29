package com.brunobrandao.expensetracker.domain.usecase

import androidx.compose.ui.graphics.Color
import com.brunobrandao.expensetracker.data.local.dao.CategoryDao
import com.brunobrandao.expensetracker.data.local.entity.CategoryEntity
import com.brunobrandao.expensetracker.data.repository.CategoryRepositoryImpl
import com.brunobrandao.expensetracker.domain.util.Clock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Testes unitários dos use cases de categoria.
 *
 * Estratégia: real CategoryRepositoryImpl + fake clock + mock DAO.
 * Isso permite verificar updatedAt=fakeNow e synced=false na camada de dados
 * sem adicionar campos de sync ao modelo de domínio.
 */
class CategoryUseCasesTest {

    private val fakeNow = 1717200000000L
    private val fakeClock = Clock { fakeNow }

    private lateinit var dao: CategoryDao
    private lateinit var createCategory: CreateCategoryUseCase
    private lateinit var updateCategory: UpdateCategoryUseCase
    private lateinit var archiveCategory: ArchiveCategoryUseCase
    private lateinit var unarchiveCategory: UnarchiveCategoryUseCase
    private lateinit var deleteCategory: DeleteCategoryUseCase

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        // Relaxed mocks return a non-null CategoryEntity for nullable returns, giving remoteId="".
        // Default: no entity exists for any key; individual tests override as needed.
        coEvery { dao.getByKey(any()) } returns null
        val repo = CategoryRepositoryImpl(dao, fakeClock)
        createCategory = CreateCategoryUseCase(repo)
        updateCategory = UpdateCategoryUseCase(repo)
        archiveCategory = ArchiveCategoryUseCase(repo)
        unarchiveCategory = UnarchiveCategoryUseCase(repo)
        deleteCategory = DeleteCategoryUseCase(repo)
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun entity(
        key: String = "test-key",
        name: String = "Test",
        isDefault: Boolean = false,
        archived: Boolean = false,
        synced: Boolean = false,
        remoteId: String? = null,
        updatedAt: Long = 0L
    ) = CategoryEntity(
        key = key, name = name, icon = "📌",
        colorArgb = 0xFF78909C.toInt(), lightColorArgb = 0xFFECEFF1.toInt(),
        isDefault = isDefault, position = 0,
        archived = archived, remoteId = remoteId,
        synced = synced, updatedAt = updatedAt
    )

    // ── CreateCategoryUseCase ─────────────────────────────────────────────────

    @Test
    fun `create generates UUID key, archived=false, synced=false, updatedAt=fakeNow`() = runTest {
        // dao.getActive() returns empty list (relaxed default)
        // dao.getByKey(uuid) returns null (relaxed default — new key never existed)

        val result = createCategory("Lazer", "🎮", Color(0xFF7F77DD), Color(0xFFEEEDFE))

        // Domain model
        assertEquals(36, result.key.length)       // UUID format: 8-4-4-4-12
        assertTrue(result.key.contains("-"))
        assertEquals("Lazer", result.name)
        assertFalse(result.archived)

        // Entity persisted in DAO
        val slot = slot<CategoryEntity>()
        coVerify(exactly = 1) { dao.upsert(capture(slot)) }
        val entity = slot.captured

        assertEquals(36, entity.key.length)
        assertFalse(entity.archived)
        assertFalse(entity.synced)
        assertEquals(fakeNow, entity.updatedAt)
        assertNull(entity.remoteId)   // new category has no remoteId yet
    }

    @Test
    fun `create trims name before saving`() = runTest {
        val result = createCategory("  Lazer  ", "🎮", Color.Red, Color.Blue)

        assertEquals("Lazer", result.name)
        val slot = slot<CategoryEntity>()
        coVerify { dao.upsert(capture(slot)) }
        assertEquals("Lazer", slot.captured.name)
    }

    @Test
    fun `create rejects empty name`() = runTest {
        try {
            createCategory("   ", "🎮", Color.Red, Color.Blue)
            fail("Expected IllegalArgumentException for empty name")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("empty") == true)
        }
    }

    @Test
    fun `create rejects duplicate name case-insensitive among active categories`() = runTest {
        coEvery { dao.getActive() } returns listOf(entity(key = "x", name = "food"))

        try {
            createCategory("Food", "🍔", Color.Red, Color.Blue)
            fail("Expected IllegalArgumentException for duplicate name")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("already exists") == true)
        }

        coVerify(exactly = 0) { dao.upsert(any()) }
    }

    @Test
    fun `create allows same name as archived category`() = runTest {
        // archived categories don't count for duplicate check
        coEvery { dao.getActive() } returns emptyList()   // archived one is not in active list

        val result = createCategory("Food", "🍔", Color.Red, Color.Blue)
        assertEquals("Food", result.name)
        coVerify(exactly = 1) { dao.upsert(any()) }
    }

    // ── UpdateCategoryUseCase ─────────────────────────────────────────────────

    @Test
    fun `update changes name and icon but keeps key, isDefault, and remoteId`() = runTest {
        val existing = entity(key = "abc123", name = "Old Name", remoteId = "remote1", isDefault = false)
        coEvery { dao.getByKey("abc123") } returns existing

        updateCategory("abc123", "New Name", "🎯", Color.Green, Color.White)

        val slot = slot<CategoryEntity>()
        coVerify(exactly = 1) { dao.upsert(capture(slot)) }
        val stored = slot.captured

        assertEquals("abc123", stored.key)           // key preserved
        assertEquals("New Name", stored.name)
        assertEquals("🎯", stored.icon)
        assertFalse(stored.isDefault)                // isDefault preserved
        assertEquals("remote1", stored.remoteId)     // remoteId preserved
        assertFalse(stored.synced)                   // marked for re-sync
        assertEquals(fakeNow, stored.updatedAt)
    }

    @Test
    fun `update rejects empty name`() = runTest {
        coEvery { dao.getByKey("abc123") } returns entity(key = "abc123")

        try {
            updateCategory("abc123", "   ", "🎯", Color.Red, Color.Blue)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("empty") == true)
        }
    }

    @Test
    fun `update throws when category not found`() = runTest {
        coEvery { dao.getByKey("missing") } returns null

        try {
            updateCategory("missing", "X", "📌", Color.Red, Color.Blue)
            fail("Expected NoSuchElementException")
        } catch (e: NoSuchElementException) {
            assertTrue(e.message?.contains("missing") == true)
        }
    }

    // ── ArchiveCategoryUseCase ────────────────────────────────────────────────

    @Test
    fun `archive sets archived=true, synced=false, updatedAt=fakeNow, preserves remoteId`() = runTest {
        val existing = entity(key = "abc123", archived = false, synced = true, remoteId = "r1")
        coEvery { dao.getByKey("abc123") } returns existing

        archiveCategory("abc123")

        val slot = slot<CategoryEntity>()
        coVerify(exactly = 1) { dao.upsert(capture(slot)) }
        val stored = slot.captured

        assertTrue(stored.archived)
        assertFalse(stored.synced)
        assertEquals(fakeNow, stored.updatedAt)
        assertEquals("r1", stored.remoteId)
    }

    @Test
    fun `archive throws when category not found`() = runTest {
        coEvery { dao.getByKey("missing") } returns null

        try {
            archiveCategory("missing")
            fail("Expected NoSuchElementException")
        } catch (e: NoSuchElementException) { /* expected */ }
    }

    // ── UnarchiveCategoryUseCase ──────────────────────────────────────────────

    @Test
    fun `unarchive sets archived=false, synced=false, updatedAt=fakeNow`() = runTest {
        val existing = entity(key = "abc123", archived = true, synced = true, remoteId = "r2")
        coEvery { dao.getByKey("abc123") } returns existing

        unarchiveCategory("abc123")

        val slot = slot<CategoryEntity>()
        coVerify(exactly = 1) { dao.upsert(capture(slot)) }
        val stored = slot.captured

        assertFalse(stored.archived)
        assertFalse(stored.synced)
        assertEquals(fakeNow, stored.updatedAt)
        assertEquals("r2", stored.remoteId)
    }

    // ── DeleteCategoryUseCase ─────────────────────────────────────────────────

    @Test
    fun `delete custom category with 0 usage succeeds`() = runTest {
        val custom = entity(key = "custom-key", isDefault = false)
        coEvery { dao.getByKey("custom-key") } returns custom
        coEvery { dao.countTransactionsUsing("custom-key") } returns 0

        // Use case is validation only; removal is the caller's responsibility.
        deleteCategory("custom-key")

        coVerify(exactly = 0) { dao.deleteByKey(any()) }
    }

    @Test
    fun `delete custom category in use throws and suggests archive`() = runTest {
        val custom = entity(key = "custom-key", isDefault = false)
        coEvery { dao.getByKey("custom-key") } returns custom
        coEvery { dao.countTransactionsUsing("custom-key") } returns 5

        try {
            deleteCategory("custom-key")
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue("Message should mention archive", e.message?.contains("archive") == true)
        }
        coVerify(exactly = 0) { dao.deleteByKey(any()) }
    }

    @Test
    fun `delete default category throws regardless of usage count`() = runTest {
        val default = entity(key = "FOOD", isDefault = true)
        coEvery { dao.getByKey("FOOD") } returns default
        coEvery { dao.countTransactionsUsing("FOOD") } returns 0  // even with 0 usage

        try {
            deleteCategory("FOOD")
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("cannot be deleted") == true)
        }
        coVerify(exactly = 0) { dao.deleteByKey(any()) }
    }

    @Test
    fun `delete throws when category not found`() = runTest {
        coEvery { dao.getByKey("missing") } returns null

        try {
            deleteCategory("missing")
            fail("Expected NoSuchElementException")
        } catch (e: NoSuchElementException) { /* expected */ }
    }

    @Test
    fun `countTransactionsUsing query covers both tables via DAO`() = runTest {
        // This test verifies that when countTransactionsUsing returns > 0,
        // delete is blocked — regardless of which table contributed the count.
        // The DAO query itself (covering both tables) is verified by the Room instrumented tests.
        val custom = entity(key = "custom-key", isDefault = false)
        coEvery { dao.getByKey("custom-key") } returns custom
        coEvery { dao.countTransactionsUsing("custom-key") } returns 1  // 1 from recurring_transactions

        try {
            deleteCategory("custom-key")
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("archive") == true)
        }
    }
}
