package com.brunobrandao.expensetracker.data.local.entity

import com.brunobrandao.expensetracker.domain.model.RecurringFrequency
import com.brunobrandao.expensetracker.domain.model.RecurringTransaction
import com.brunobrandao.expensetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class RecurringTransactionMapperTest {

    private val sampleEntity = RecurringTransactionEntity(
        id = 1L,
        description = "Netflix",
        amount = 39.90,
        type = TransactionType.EXPENSE,
        category = "OTHER",
        note = "Streaming",
        frequency = RecurringFrequency.MONTHLY,
        startDate = 1700000000000L,
        nextDueDate = 1702678400000L,
        active = true
    )

    private val sampleDomain = RecurringTransaction(
        id = 1L,
        description = "Netflix",
        amount = 39.90,
        type = TransactionType.EXPENSE,
        category = "OTHER",
        note = "Streaming",
        frequency = RecurringFrequency.MONTHLY,
        startDate = 1700000000000L,
        nextDueDate = 1702678400000L,
        active = true
    )

    @Test
    fun `entity toDomain maps all fields correctly`() {
        val result = sampleEntity.toDomain()

        assertEquals(sampleEntity.id, result.id)
        assertEquals(sampleEntity.description, result.description)
        assertEquals(sampleEntity.amount, result.amount, 0.001)
        assertEquals(sampleEntity.type, result.type)
        assertEquals(sampleEntity.category, result.category)
        assertEquals(sampleEntity.note, result.note)
        assertEquals(sampleEntity.frequency, result.frequency)
        assertEquals(sampleEntity.startDate, result.startDate)
        assertEquals(sampleEntity.nextDueDate, result.nextDueDate)
        assertEquals(sampleEntity.active, result.active)
    }

    @Test
    fun `domain toEntity maps all fields correctly`() {
        val result = sampleDomain.toEntity()

        assertEquals(sampleDomain.id, result.id)
        assertEquals(sampleDomain.description, result.description)
        assertEquals(sampleDomain.amount, result.amount, 0.001)
        assertEquals(sampleDomain.type, result.type)
        assertEquals(sampleDomain.category, result.category)
        assertEquals(sampleDomain.note, result.note)
        assertEquals(sampleDomain.frequency, result.frequency)
        assertEquals(sampleDomain.startDate, result.startDate)
        assertEquals(sampleDomain.nextDueDate, result.nextDueDate)
        assertEquals(sampleDomain.active, result.active)
    }

    @Test
    fun `entity to domain and back preserves data`() {
        val domain = sampleEntity.toDomain()
        val backToEntity = domain.toEntity()
        assertEquals(sampleEntity, backToEntity.copy(updatedAt = sampleEntity.updatedAt))
    }

    @Test
    fun `domain to entity and back preserves data`() {
        val entity = sampleDomain.toEntity()
        val backToDomain = entity.toDomain()

        assertEquals(sampleDomain, backToDomain)
    }

    @Test
    fun `maps all frequencies correctly`() {
        RecurringFrequency.entries.forEach { freq ->
            val entity = sampleEntity.copy(frequency = freq)
            val domain = entity.toDomain()
            assertEquals(freq, domain.frequency)
        }
    }

    @Test
    fun `inactive rule maps correctly`() {
        val entity = sampleEntity.copy(active = false)
        val domain = entity.toDomain()
        assertEquals(false, domain.active)
    }
}
