package com.brunobrandao.expensetracker

// Teste INSTRUMENTADO — requer emulador ou device conectado.
// Rodar com: ./gradlew connectedDebugAndroidTest
// NÃO roda com testDebugUnitTest (unit test sem device).

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.brunobrandao.expensetracker.data.local.AppDatabase
import org.junit.Assert.assertEquals
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
}
