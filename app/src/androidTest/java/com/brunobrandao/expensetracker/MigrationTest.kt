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

    /**
     * Cria o DB na versão 2, insere uma transação com as colunas exatas do schema v2,
     * migra para v3 e valida que:
     *  - a transação sobreviveu com os mesmos valores
     *  - a coluna recurringId existe e está NULL para ela
     *  - a tabela recurring_transactions foi criada
     */
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
            assertTrue("Transação deve sobreviver à migração 2→3", cursor.moveToFirst())
            assertEquals(5000.0, cursor.getDouble(cursor.getColumnIndexOrThrow("amount")), 0.001)
            assertTrue(
                "recurringId deve ser NULL após migração",
                cursor.isNull(cursor.getColumnIndexOrThrow("recurringId"))
            )
        }

        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='recurring_transactions'").use { cursor ->
            assertTrue("Tabela recurring_transactions deve existir após migração", cursor.moveToFirst())
        }
    }

    /**
     * Cria o DB na versão 1, insere uma transação com as colunas exatas do schema v1,
     * migra de 1→2→3 e valida que:
     *  - a transação sobreviveu com os mesmos valores
     *  - remoteId ficou com default ''
     *  - synced ficou com default 0
     *  - updatedAt ficou com default 0
     *  - recurringId é NULL
     */
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
            assertTrue("Transação deve sobreviver às migrações 1→2→3", cursor.moveToFirst())
            assertEquals(3000.0, cursor.getDouble(cursor.getColumnIndexOrThrow("amount")), 0.001)
            assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("remoteId")))
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("synced")))
            assertEquals(0L, cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt")))
            assertTrue(
                "recurringId deve ser NULL",
                cursor.isNull(cursor.getColumnIndexOrThrow("recurringId"))
            )
        }
    }

    /**
     * Cria o DB na versão 3, insere uma transação com categoria do enum ANTIGO (ex: "FOOD"),
     * aplica MIGRATION_3_4 e valida:
     *  (a) a transação sobreviveu com os mesmos valores
     *  (b) o campo category continua "FOOD" — resolve pra categoria válida, NÃO cai em fallback OTHER
     *  (c) a tabela categories foi criada com schema correto: remoteId nullable, synced/updatedAt com DEFAULT 0
     */
    @Test
    fun migrate3to4_keepsData() {
        helper.createDatabase(TEST_DB, 3).use { db ->
            db.execSQL(
                """INSERT INTO transactions
                   (description, amount, type, category, date, note, createdAt, remoteId, synced, updatedAt)
                   VALUES ('Almoço', 42.50, 'EXPENSE', 'FOOD', 1717200000000, '', 1717200000000, '', 0, 1717200000000)"""
            )
            // Insere também uma recurring para validar que recurring_transactions sobrevive
            db.execSQL(
                """INSERT INTO recurring_transactions
                   (description, amount, type, category, note, frequency, startDate, nextDueDate, active, remoteId, synced, updatedAt)
                   VALUES ('Netflix', 39.90, 'EXPENSE', 'ENTERTAINMENT', '', 'MONTHLY', 1717200000000, 1717200000000, 1, '', 0, 0)"""
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true, AppDatabase.MIGRATION_3_4)

        // (a) + (b) — transação sobrevive com category == "FOOD" (mesmo valor do enum antigo)
        db.query("SELECT * FROM transactions WHERE description = 'Almoço'").use { cursor ->
            assertTrue("Transação deve sobreviver à migração 3→4", cursor.moveToFirst())
            assertEquals(42.50, cursor.getDouble(cursor.getColumnIndexOrThrow("amount")), 0.001)

            val categoryKey = cursor.getString(cursor.getColumnIndexOrThrow("category"))
            assertEquals("FOOD", categoryKey)

            // (b) — resolve pra categoria válida, não é fallback
            val resolved = DefaultCategories.LIST.find { it.key == categoryKey }
            assertNotNull("Categoria '$categoryKey' deve existir em DefaultCategories", resolved)
            assertFalse("Categoria não deve ser o fallback genérico", resolved!!.key == "OTHER" && !resolved.isDefault)
            assertEquals("Food", resolved.name)
        }

        // recurring_transactions também sobrevive
        db.query("SELECT category FROM recurring_transactions WHERE description = 'Netflix'").use { cursor ->
            assertTrue("Recurring deve sobreviver à migração 3→4", cursor.moveToFirst())
            assertEquals("ENTERTAINMENT", cursor.getString(cursor.getColumnIndexOrThrow("category")))
        }

        // (c) — tabela categories foi criada
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='categories'").use { cursor ->
            assertTrue("Tabela categories deve existir após migração", cursor.moveToFirst())
        }

        // (c) — schema correto: remoteId nullable (INSERT sem remoteId deve funcionar)
        db.execSQL(
            """INSERT INTO categories (key, name, icon, colorArgb, lightColorArgb, isDefault, position)
               VALUES ('FOOD', 'Food', '🍔', -2662352, -344345, 1, 0)"""
        )
        db.query("SELECT synced, updatedAt, remoteId FROM categories WHERE key='FOOD'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("synced deve ter DEFAULT 0", 0, cursor.getInt(cursor.getColumnIndexOrThrow("synced")))
            assertEquals("updatedAt deve ter DEFAULT 0", 0L, cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt")))
            assertTrue("remoteId deve ser NULL quando não fornecido", cursor.isNull(cursor.getColumnIndexOrThrow("remoteId")))
        }
    }

    /**
     * Cadeia completa 1→4: cenário real de um usuário com app publicado (v1) atualizando para v4.
     * Transação criada na v1 com categoria do enum antigo ("SALARY") sobrevive a todas as migrações
     * e a categoria resolve corretamente na v4.
     */
    @Test
    fun migrateAll1to4() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            // v1 só tem: id, description, amount, type, category, date, note, createdAt
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
            assertTrue("Transação v1 deve sobreviver à cadeia completa 1→4", cursor.moveToFirst())
            assertEquals(8000.0, cursor.getDouble(cursor.getColumnIndexOrThrow("amount")), 0.001)

            val categoryKey = cursor.getString(cursor.getColumnIndexOrThrow("category"))
            assertEquals("SALARY", categoryKey)

            // Campos adicionados em migrações intermediárias devem ter os defaults corretos
            assertEquals("remoteId deve ser ''", "", cursor.getString(cursor.getColumnIndexOrThrow("remoteId")))
            assertEquals("synced deve ser 0", 0, cursor.getInt(cursor.getColumnIndexOrThrow("synced")))
            assertTrue("recurringId deve ser NULL", cursor.isNull(cursor.getColumnIndexOrThrow("recurringId")))

            // Categoria resolve corretamente via DefaultCategories
            val resolved = DefaultCategories.LIST.find { it.key == categoryKey }
            assertNotNull("SALARY deve existir em DefaultCategories", resolved)
            assertEquals("Salary", resolved!!.name)
        }

        // Tabela categories deve existir ao final da cadeia
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='categories'").use { cursor ->
            assertTrue("Tabela categories deve existir na v4", cursor.moveToFirst())
        }
    }
}
