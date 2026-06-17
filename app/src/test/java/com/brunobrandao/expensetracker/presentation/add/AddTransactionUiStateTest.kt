package com.brunobrandao.expensetracker.presentation.add

import com.brunobrandao.expensetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddTransactionUiStateTest {

    @Test
    fun `default state has correct values`() {
        val state = AddTransactionUiState()

        assertNull(state.editingId)
        assertEquals("", state.description)
        assertEquals("", state.amount)
        assertEquals(TransactionType.EXPENSE, state.type)
        assertEquals("OTHER", state.category)
        assertEquals("", state.note)
        assertFalse(state.isLoading)
        assertFalse(state.isSaved)
        assertNull(state.errorMessage)
    }

    @Test
    fun `isEditing returns true when editingId is set`() {
        val state = AddTransactionUiState(editingId = 5L)

        assertTrue(state.isEditing)
    }

    @Test
    fun `isEditing returns false when editingId is null`() {
        val state = AddTransactionUiState(editingId = null)

        assertFalse(state.isEditing)
    }
}
