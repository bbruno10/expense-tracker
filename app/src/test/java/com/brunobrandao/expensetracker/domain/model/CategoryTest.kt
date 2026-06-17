package com.brunobrandao.expensetracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryTest {

    @Test
    fun `DefaultCategories LIST has 11 entries`() {
        assertEquals(11, DefaultCategories.LIST.size)
    }

    @Test
    fun `all default categories have non-empty names`() {
        DefaultCategories.LIST.forEach { category ->
            assertTrue("Category ${category.key} should have a non-empty name", category.name.isNotBlank())
        }
    }

    @Test
    fun `all default categories have non-empty icons`() {
        DefaultCategories.LIST.forEach { category ->
            assertTrue("Category ${category.key} should have a non-empty icon", category.icon.isNotBlank())
        }
    }

    @Test
    fun `all default categories have unique keys`() {
        val keys = DefaultCategories.LIST.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `all default categories have unique names`() {
        val names = DefaultCategories.LIST.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun `expense categories exist`() {
        val expectedKeys = listOf("FOOD", "TRANSPORT", "HOUSING", "HEALTH", "EDUCATION",
            "ENTERTAINMENT", "SHOPPING", "BILLS")
        val keys = DefaultCategories.LIST.map { it.key }.toSet()
        expectedKeys.forEach { key ->
            assertTrue("Category $key should exist", key in keys)
        }
    }

    @Test
    fun `income categories exist`() {
        val expectedKeys = listOf("SALARY", "INVESTMENT")
        val keys = DefaultCategories.LIST.map { it.key }.toSet()
        expectedKeys.forEach { key ->
            assertTrue("Category $key should exist", key in keys)
        }
    }

    @Test
    fun `each default category has distinct color and lightColor`() {
        DefaultCategories.LIST.forEach { category ->
            assertNotEquals(
                "Category ${category.key} should have different color and lightColor",
                category.color,
                category.lightColor
            )
        }
    }

    @Test
    fun `all default categories have isDefault = true`() {
        DefaultCategories.LIST.forEach { category ->
            assertTrue("Category ${category.key} should be a default", category.isDefault)
        }
    }

    @Test
    fun `fallback returns category with given key as name`() {
        val fallback = DefaultCategories.fallback("CUSTOM_CAT")
        assertEquals("CUSTOM_CAT", fallback.key)
        assertEquals("CUSTOM_CAT", fallback.name)
        assertFalse(fallback.isDefault)
    }
}
