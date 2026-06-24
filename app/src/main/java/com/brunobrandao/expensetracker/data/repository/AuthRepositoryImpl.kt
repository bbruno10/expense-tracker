package com.brunobrandao.expensetracker.data.repository

import com.brunobrandao.expensetracker.domain.repository.AuthRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    override val currentUserEmail: String?
        get() = auth.currentUser?.email

    override val isAuthenticated: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser != null)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signUp(email: String, password: String): Result<Unit> =
        runCatching {
            auth.createUserWithEmailAndPassword(email.trim(), password).await()
            Unit
        }

    override suspend fun signIn(email: String, password: String): Result<Unit> =
        runCatching {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
            Unit
        }

    override fun signOut() = auth.signOut()

    override suspend fun reauthenticate(password: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("No authenticated user")
        val email = user.email ?: error("User has no email")
        val credential = EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential).await()
        Unit
    }

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("No authenticated user")
        user.delete().await()
        Unit
    }
}
