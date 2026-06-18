package com.brunobrandao.expensetracker.presentation.settings

import com.brunobrandao.expensetracker.domain.model.Transaction
import com.brunobrandao.expensetracker.domain.model.TransactionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExportTest {

    private fun transaction(
        category: String,
        description: String = "Test",
        amount: Double = 10.0,
        type: TransactionType = TransactionType.EXPENSE,
        note: String = "",
        date: Long = 1700000000000L
    ) = Transaction(
        id = 1L,
        description = description,
        amount = amount,
        type = type,
        category = category,
        date = date,
        note = note
    )

    @Test
    fun `custom category key resolves to name not UUID`() {
        val uuid = "f47ac10b-58cc-4372-a567-0e02b2c3d479"
        val categoryNames = mapOf(uuid to "Hobbies")

        val csv = buildCsvContent(listOf(transaction(uuid)), categoryNames)

        assertTrue("Should contain category name", csv.contains("Hobbies"))
        assertFalse("Should not contain raw UUID", csv.contains(uuid))
    }

    @Test
    fun `default category key resolves to name`() {
        val categoryNames = mapOf("FOOD" to "Food", "SALARY" to "Salary")

        val csv = buildCsvContent(listOf(transaction("FOOD")), categoryNames)

        assertTrue(csv.contains("Food"))
        assertFalse("Should not contain raw key", csv.contains(",FOOD,"))
    }

    @Test
    fun `orphan key falls back to key without crashing`() {
        val orphanKey = "deleted-category-uuid"
        val categoryNames = emptyMap<String, String>()

        val csv = buildCsvContent(listOf(transaction(orphanKey)), categoryNames)

        assertTrue("Fallback should use the raw key", csv.contains(orphanKey))
    }

    @Test
    fun `header row is always present`() {
        val csv = buildCsvContent(emptyList(), emptyMap())

        assertTrue(csv.startsWith("Date,Type,Category,Description,Amount,Note"))
    }

    @Test
    fun `expense amount is prefixed with minus`() {
        val categoryNames = mapOf("OTHER" to "Other")
        val csv = buildCsvContent(
            listOf(transaction("OTHER", amount = 42.5, type = TransactionType.EXPENSE)),
            categoryNames
        )

        assertTrue(csv.contains("-42.5"))
    }

    @Test
    fun `income amount has no minus prefix`() {
        val categoryNames = mapOf("SALARY" to "Salary")
        val csv = buildCsvContent(
            listOf(transaction("SALARY", amount = 3000.0, type = TransactionType.INCOME)),
            categoryNames
        )

        assertTrue(csv.contains("3000.0"))
        assertFalse(csv.contains("-3000.0"))
    }

    @Test
    fun `description with quotes is escaped`() {
        val categoryNames = mapOf("OTHER" to "Other")
        val csv = buildCsvContent(
            listOf(transaction("OTHER", description = "He said \"hello\"")),
            categoryNames
        )

        assertTrue(csv.contains("\"He said \"\"hello\"\"\""))
    }
}
