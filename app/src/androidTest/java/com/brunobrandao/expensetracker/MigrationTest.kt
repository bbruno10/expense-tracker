package com.brunobrandao.expensetracker

// Teste INSTRUMENTADO — requer emulador ou device conectado.
// Rodar com: ./gradlew connectedDebugAndroidTest
// NÃO roda com testDebugUnitTest (unit test sem device).

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.brunobrandao.expensetracker.data.local.AppDatabase
import com.brunobrandao.expensetracker.domain.model.DefaultCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    companion object {
        private const val TEST_DB = "migration-test"
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate2to3_keepsData() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL(
                """INSERT INTO transactions
                   (description, amount, type, category, date, note, createdAt, remoteId, synced, updatedAt)
                   VALUES ('Salário', 5000.0, 'INCOME', 'SALARY', 1717200000000, '', 1717200000000, '', 0, 1717200000000)"""
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, AppDatabase.MIGRATION_2_3)

        db.query("SELECT * FROM transactions WHERE description = 'Salário'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(5000.0, cursor.getDouble(cursor.getColumnIndexOrThrow("amount")), 0.001)
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("recurringId")))
        }

        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='recurring_transactions'").use { cursor ->
            assertTrue(cursor.moveToFirst())
        }
    }

    @Test
    fun migrateAll1to3() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                """INSERT INTO transactions
                   (description, amount, type, category, date, note, createdAt)
                   VALUES ('Salário V1', 3000.0, 'INCOME', 'SALARY', 1717200000000, '', 1717200000000)"""
            )
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB, 3, true,
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3
        )

        db.query("SELECT * FROM transactions WHERE description = 'Salário V1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("remoteId")))
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("synced")))
            assertEquals(0L, cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt")))
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("recurringId")))
        }
    }

    @Test
    fun migrate3to4_keepsData() {
        helper.createDatabase(TEST_DB, 3).use { db ->
            db.execSQL(
                """INSERT INTO transactions
                   (description, amount, type, category, date, note, createdAt, remoteId, synced, updatedAt)
                   VALUES ('Almoço', 42.50, 'EXPENSE', 'FOOD', 1717200000000, '', 1717200000000, '', 0, 1717200000000)"""
            )
            db.execSQL(
                """INSERT INTO recurring_transactions
                   (description, amount, type, category, note, frequency, startDate, nextDueDate, active, remoteId, synced, updatedAt)
                   VALUES ('Netflix', 39.90, 'EXPENSE', 'ENTERTAINMENT', '', 'MONTHLY', 1717200000000, 1717200000000, 1, '', 0, 0)"""
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true, AppDatabase.MIGRATION_3_4)

        db.query("SELECT * FROM transactions WHERE description = 'Almoço'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            val categoryKey = cursor.getString(cursor.getColumnIndexOrThrow("category"))
            assertEquals("FOOD", categoryKey)

            val resolved = DefaultCategories.LIST.find { it.key == categoryKey }
            assertNotNull(resolved)
            assertFalse(resolved!!.key == "OTHER" && !resolved.isDefault)
            assertEquals("Food", resolved.name)
        }

        db.query("SELECT category FROM recurring_transactions WHERE description = 'Netflix'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("ENTERTAINMENT", cursor.getString(cursor.getColumnIndexOrThrow("category")))
        }

        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='categories'").use { cursor ->
            assertTrue(cursor.moveToFirst())
        }

        db.execSQL(
            """INSERT INTO categories (key, name, icon, colorArgb, lightColorArgb, isDefault, position)
               VALUES ('FOOD', 'Food', '🍔', -2662352, -344345, 1, 0)"""
        )
        db.query("SELECT synced, updatedAt, remoteId FROM categories WHERE key='FOOD'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("synced")))
            assertEquals(0L, cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt")))
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("remoteId")))
        }
    }

    @Test
    fun migrateAll1to4() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                """INSERT INTO transactions
                   (description, amount, type, category, date, note, createdAt)
                   VALUES ('Salário V1→4', 8000.0, 'INCOME', 'SALARY', 1717200000000, '', 1717200000000)"""
            )
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB, 4, true,
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4
        )

        db.query("SELECT * FROM transactions WHERE description = 'Salário V1→4'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            val categoryKey = cursor.getString(cursor.getColumnIndexOrThrow("category"))
            assertEquals("SALARY", categoryKey)
            assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("remoteId")))
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("synced")))
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("recurringId")))

            val resolved = DefaultCategories.LIST.find { it.key == categoryKey }
            assertNotNull(resolved)
            assertEquals("Salary", resolved!!.name)
        }

        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='categories'").use { cursor ->
            assertTrue(cursor.moveToFirst())
        }
    }

    /**
     * Cria o DB na v4, semeia uma categoria, aplica MIGRATION_4_5 e valida:
     *  (a) a categoria sobreviveu
     *  (b) a coluna archived existe com DEFAULT 0
     *  (c) novas categorias inseridas sem archived recebem 0
     */
    @Test
    fun migrate4to5_keepsData() {
        helper.createDatabase(TEST_DB, 4).use { db ->
            // Seed uma categoria na v4 (sem a coluna archived — não existe ainda)
            db.execSQL(
                """INSERT INTO categories (key, name, icon, colorArgb, lightColorArgb, isDefault, position, synced, updatedAt)
                   VALUES ('FOOD', 'Food', '🍔', -2662352, -344345, 1, 0, 0, 0)"""
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 5, true, AppDatabase.MIGRATION_4_5)

        // (a) categoria sobreviveu
        db.query("SELECT * FROM categories WHERE key='FOOD'").use { cursor ->
            assertTrue("Categoria seedada na v4 deve sobreviver à migração 4→5", cursor.moveToFirst())
            assertEquals("Food", cursor.getString(cursor.getColumnIndexOrThrow("name")))

            // (b) coluna archived existe e tem DEFAULT 0 para linhas pré-existentes
            val archivedIdx = cursor.getColumnIndexOrThrow("archived")
            assertEquals("Categoria existente deve ter archived=0 após migração", 0, cursor.getInt(archivedIdx))
        }

        // (c) nova categoria inserida SEM archived também recebe DEFAULT 0
        db.execSQL(
            """INSERT INTO categories (key, name, icon, colorArgb, lightColorArgb, isDefault, position)
               VALUES ('TRANSPORT', 'Transport', '🚗', -13459043, -1710085, 1, 1)"""
        )
        db.query("SELECT archived FROM categories WHERE key='TRANSPORT'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Nova categoria sem archived explícito deve ter DEFAULT 0", 0, cursor.getInt(0))
        }
    }

    /**
     * Cadeia completa 1→5: cenário real de um usuário com app publicado (v1) atualizando para v5.
     */
    @Test
    fun migrateAll1to5() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                """INSERT INTO transactions
                   (description, amount, type, category, date, note, createdAt)
                   VALUES ('Salário V1→5', 9000.0, 'INCOME', 'SALARY', 1717200000000, '', 1717200000000)"""
            )
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB, 5, true,
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5
        )

        // Transação sobreviveu à cadeia completa
        db.query("SELECT * FROM transactions WHERE description = 'Salário V1→5'").use { cursor ->
            assertTrue("Transação v1 deve sobreviver à cadeia 1→5", cursor.moveToFirst())
            assertEquals(9000.0, cursor.getDouble(cursor.getColumnIndexOrThrow("amount")), 0.001)
            assertEquals("SALARY", cursor.getString(cursor.getColumnIndexOrThrow("category")))
            assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("remoteId")))
        }

        // Tabela categories existe com coluna archived na v5
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='categories'").use { cursor ->
            assertTrue("Tabela categories deve existir na v5", cursor.moveToFirst())
        }

        // Podemos inserir uma categoria com archived explícito
        db.execSQL(
            """INSERT INTO categories (key, name, icon, colorArgb, lightColorArgb, isDefault, position, archived)
               VALUES ('CUSTOM', 'Custom', '📌', -8092539, -1381654, 0, 100, 0)"""
        )
        db.query("SELECT archived FROM categories WHERE key='CUSTOM'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }
}
