package com.brunobrandao.expensetracker.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserId: String?
    val currentUserEmail: String?
    val isAuthenticated: Flow<Boolean>
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Unit>
    fun signOut()
    /** Re-authenticates the current user with their password. Must be called before deleteAccount(). */
    suspend fun reauthenticate(password: String): Result<Unit>
    /** Deletes the Firebase Auth account. Caller must call reauthenticate() first. */
    suspend fun deleteAccount(): Result<Unit>
}
