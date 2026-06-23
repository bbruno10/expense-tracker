package com.brunobrandao.expensetracker.presentation.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brunobrandao.expensetracker.data.preferences.UserPreferencesRepository
import com.brunobrandao.expensetracker.data.sync.SyncRepository
import com.brunobrandao.expensetracker.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val isLoggedIn: Boolean
        get() = authRepository.currentUserId != null

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun signIn(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (!Patterns.EMAIL_ADDRESS.matcher(state.email.trim()).matches()) {
            _uiState.update { it.copy(errorMessage = "Invalid email address") }
            return
        }
        if (state.password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.signIn(state.email, state.password)
                .onSuccess { onSuccess() }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = friendlyError(error)) }
                }
        }
    }

    fun signUp(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (!Patterns.EMAIL_ADDRESS.matcher(state.email.trim()).matches()) {
            _uiState.update { it.copy(errorMessage = "Invalid email address") }
            return
        }
        if (state.password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters") }
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.signUp(state.email, state.password)
                .onSuccess { onSuccess() }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = friendlyError(error)) }
                }
        }
    }

    /**
     * Stops the Firestore listener, then in the background:
     * tries to push any unsynced rows before signing out Firebase Auth.
     * Room is cleared only if all rows were successfully synced.
     * If offline, Room is left intact for the next session.
     */
    fun signOut() {
        val userId = authRepository.currentUserId
        syncRepository.stopSync()
        viewModelScope.launch {
            if (userId != null) {
                try { syncRepository.pushPendingAndClear(userId) } catch (_: Exception) {}
            }
            authRepository.signOut()
            preferencesRepository.clearCurrency()
        }
    }

    private fun friendlyError(error: Throwable): String = when (error) {
        is FirebaseAuthInvalidCredentialsException -> "Invalid email or password"
        is FirebaseAuthUserCollisionException -> "An account with this email already exists"
        is FirebaseAuthWeakPasswordException -> "Password must be at least 6 characters"
        else -> error.message ?: "An error occurred. Please try again."
    }
}
