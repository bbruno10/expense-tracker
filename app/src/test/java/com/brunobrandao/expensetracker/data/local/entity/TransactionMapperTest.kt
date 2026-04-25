package com.brunobrandao.expensetracker.data.local.entity

import com.brunobrandao.expensetracker.domain.model.Category
import com.brunobrandao.expensetracker.domain.model.Transaction
import com.brunobrandao.expensetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionMapperTest {

    private val sampleEntity = TransactionEntity(
        id = 1L,
        description = "Lunch",
        amount = 25.50,
        type = TransactionType.EXPENSE,
        category = Category.FOOD,
        date = 1700000000000L,
        note = "Restaurant downtown",
        createdAt = 1700000000000L
    )

    private val sampleDomain = Transaction(
        id = 1L,
        description = "Lunch",
        amount = 25.50,
        type = TransactionType.EXPENSE,
        category = Category.FOOD,
        date = 1700000000000L,
        note = "Restaurant downtown",
        createdAt = 1700000000000L
    )

    @Test
    fun `entity toDomain maps all fields correctly`() {
        val result = sampleEntity.toDomain()

        assertEquals(sampleEntity.id, result.id)
        assertEquals(sampleEntity.description, result.description)
        assertEquals(sampleEntity.amount, result.amount, 0.001)
        assertEquals(sampleEntity.type, result.type)
        assertEquals(sampleEntity.category, result.category)
        assertEquals(sampleEntity.date, result.date)
        assertEquals(sampleEntity.note, result.note)
        assertEquals(sampleEntity.createdAt, result.createdAt)
    }

    @Test
    fun `domain toEntity maps all fields correctly`() {
        val result = sampleDomain.toEntity()

        assertEquals(sampleDomain.id, result.id)
        assertEquals(sampleDomain.description, result.description)
        assertEquals(sampleDomain.amount, result.amount, 0.001)
        assertEquals(sampleDomain.type, result.type)
        assertEquals(sampleDomain.category, result.category)
        assertEquals(sampleDomain.date, result.date)
        assertEquals(sampleDomain.note, result.note)
        assertEquals(sampleDomain.createdAt, result.createdAt)
    }

    @Test
    fun `entity to domain and back preserves data`() {
        val domain = sampleEntity.toDomain()
        val backToEntity = domain.toEntity()

        assertEquals(sampleEntity, backToEntity)
    }

    @Test
    fun `domain to entity and back preserves data`() {
        val entity = sampleDomain.toEntity()
        val backToDomain = entity.toDomain()

        assertEquals(sampleDomain, backToDomain)
    }

    @Test
    fun `maps entity with default values correctly`() {
        val minimalEntity = TransactionEntity(
            description = "Test",
            amount = 10.0,
            type = TransactionType.INCOME,
            category = Category.SALARY,
            date = 1700000000000L
        )

        val domain = minimalEntity.toDomain()

        assertEquals(0L, domain.id)
        assertEquals("", domain.note)
        assertEquals("Test", domain.description)
        assertEquals(10.0, domain.amount, 0.001)
    }

    @Test
    fun `maps all transaction types correctly`() {
        TransactionType.entries.forEach { type ->
            val entity = sampleEntity.copy(type = type)
            val domain = entity.toDomain()
            assertEquals(type, domain.type)
        }
    }

    @Test
    fun `maps all categories correctly`() {
        Category.entries.forEach { category ->
            val entity = sampleEntity.copy(category = category)
            val domain = entity.toDomain()
            assertEquals(category, domain.category)
        }
    }
}
