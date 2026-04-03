package com.example.expensetracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryTest {

    @Test
    fun `all categories have non-empty display names`() {
        Category.entries.forEach { category ->
            assertTrue(
                "Category ${category.name} should have a non-empty displayName",
                category.displayName.isNotBlank()
            )
        }
    }

    @Test
    fun `all categories have non-empty icons`() {
        Category.entries.forEach { category ->
            assertTrue(
                "Category ${category.name} should have a non-empty icon",
                category.icon.isNotBlank()
            )
        }
    }

    @Test
    fun `category count is 11`() {
        assertEquals(11, Category.entries.size)
    }

    @Test
    fun `all categories have unique display names`() {
        val displayNames = Category.entries.map { it.displayName }
        assertEquals(displayNames.size, displayNames.toSet().size)
    }

    @Test
    fun `expense categories exist`() {
        val expectedExpense = listOf("FOOD", "TRANSPORT", "HOUSING", "HEALTH", "EDUCATION",
            "ENTERTAINMENT", "SHOPPING", "BILLS")
        expectedExpense.forEach { name ->
            assertTrue(
                "Category $name should exist",
                Category.entries.any { it.name == name }
            )
        }
    }

    @Test
    fun `income categories exist`() {
        val expectedIncome = listOf("SALARY", "INVESTMENT")
        expectedIncome.forEach { name ->
            assertTrue(
                "Category $name should exist",
                Category.entries.any { it.name == name }
            )
        }
    }

    @Test
    fun `each category has distinct color and lightColor`() {
        Category.entries.forEach { category ->
            assertNotEquals(
                "Category ${category.name} should have different color and lightColor",
                category.color,
                category.lightColor
            )
        }
    }
}
