package com.brunobrandao.expensetracker.presentation.history

import com.brunobrandao.expensetracker.domain.model.Transaction

fun String.normalizeForSearch(): String =
    java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()

object TransactionSearchMatcher {
    fun matches(transaction: Transaction, query: String, scope: SearchScope): Boolean {
        val normalizedQuery = query.trim().normalizeForSearch()
        if (normalizedQuery.isEmpty()) return true
        return when (scope) {
            SearchScope.TITLE -> transaction.description.normalizeForSearch().contains(normalizedQuery)
            SearchScope.NOTE  -> transaction.note.normalizeForSearch().contains(normalizedQuery)
            SearchScope.BOTH  ->
                transaction.description.normalizeForSearch().contains(normalizedQuery) ||
                transaction.note.normalizeForSearch().contains(normalizedQuery)
        }
    }
}
