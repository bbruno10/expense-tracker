package com.brunobrandao.expensetracker.presentation.history

import com.brunobrandao.expensetracker.domain.model.Transaction
import com.brunobrandao.expensetracker.domain.model.TransactionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionSearchMatcherTest {

    private fun transaction(description: String, note: String = "") = Transaction(
        id = 1L,
        description = description,
        amount = 10.0,
        type = TransactionType.EXPENSE,
        category = "FOOD",
        date = 0L,
        note = note
    )

    // ── Empty query ───────────────────────────────────────────────────────────

    @Test
    fun `empty query matches any transaction`() {
        assertTrue(TransactionSearchMatcher.matches(transaction("Anything"), "", SearchScope.BOTH))
    }

    @Test
    fun `blank query matches any transaction`() {
        assertTrue(TransactionSearchMatcher.matches(transaction("Anything"), "   ", SearchScope.BOTH))
    }

    // ── Case insensitive ──────────────────────────────────────────────────────

    @Test
    fun `match is case-insensitive`() {
        val t = transaction("Salary Payment")
        assertTrue(TransactionSearchMatcher.matches(t, "salary", SearchScope.TITLE))
        assertTrue(TransactionSearchMatcher.matches(t, "SALARY", SearchScope.TITLE))
        assertTrue(TransactionSearchMatcher.matches(t, "SaLaRy", SearchScope.TITLE))
    }

    // ── Accent insensitive ────────────────────────────────────────────────────

    @Test
    fun `accent-insensitive — query without accent matches accented title`() {
        val t = transaction("Cartão de crédito")
        assertTrue(TransactionSearchMatcher.matches(t, "cartao", SearchScope.TITLE))
    }

    @Test
    fun `accent-insensitive — query with accent matches non-accented title`() {
        val t = transaction("Restaurante")
        assertTrue(TransactionSearchMatcher.matches(t, "réstaurante", SearchScope.TITLE))
    }

    @Test
    fun `accent-insensitive — note field`() {
        val t = transaction("Lunch", note = "pagamento via Pix")
        assertTrue(TransactionSearchMatcher.matches(t, "pagamento via pix", SearchScope.NOTE))
    }

    // ── TITLE scope ───────────────────────────────────────────────────────────

    @Test
    fun `TITLE scope matches when query is in description`() {
        val t = transaction("Grocery shopping", note = "weekly essentials")
        assertTrue(TransactionSearchMatcher.matches(t, "grocery", SearchScope.TITLE))
    }

    @Test
    fun `TITLE scope does NOT match when query is only in note`() {
        val t = transaction("Grocery shopping", note = "weekly essentials")
        assertFalse(TransactionSearchMatcher.matches(t, "essentials", SearchScope.TITLE))
    }

    // ── NOTE scope ────────────────────────────────────────────────────────────

    @Test
    fun `NOTE scope matches when query is in note`() {
        val t = transaction("Grocery shopping", note = "weekly essentials")
        assertTrue(TransactionSearchMatcher.matches(t, "essentials", SearchScope.NOTE))
    }

    @Test
    fun `NOTE scope does NOT match when query is only in description`() {
        val t = transaction("Grocery shopping", note = "weekly essentials")
        assertFalse(TransactionSearchMatcher.matches(t, "grocery", SearchScope.NOTE))
    }

    // ── BOTH scope ────────────────────────────────────────────────────────────

    @Test
    fun `BOTH scope matches query found in description`() {
        val t = transaction("Netflix subscription", note = "monthly plan")
        assertTrue(TransactionSearchMatcher.matches(t, "netflix", SearchScope.BOTH))
    }

    @Test
    fun `BOTH scope matches query found in note`() {
        val t = transaction("Netflix subscription", note = "monthly plan")
        assertTrue(TransactionSearchMatcher.matches(t, "monthly", SearchScope.BOTH))
    }

    @Test
    fun `BOTH scope does not match when query is in neither field`() {
        val t = transaction("Netflix subscription", note = "monthly plan")
        assertFalse(TransactionSearchMatcher.matches(t, "spotify", SearchScope.BOTH))
    }

    // ── No match ─────────────────────────────────────────────────────────────

    @Test
    fun `returns false when query not found anywhere`() {
        val t = transaction("Coffee", note = "morning")
        assertFalse(TransactionSearchMatcher.matches(t, "dinner", SearchScope.BOTH))
    }
}
