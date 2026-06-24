package com.brunobrandao.expensetracker.presentation.settings

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brunobrandao.expensetracker.data.preferences.Currency
import com.brunobrandao.expensetracker.data.preferences.ThemeMode
import com.brunobrandao.expensetracker.data.preferences.UserPreferencesRepository
import com.brunobrandao.expensetracker.data.sync.SyncRepository
import com.brunobrandao.expensetracker.domain.model.Transaction
import com.brunobrandao.expensetracker.domain.model.TransactionType
import com.brunobrandao.expensetracker.domain.repository.AuthRepository
import com.brunobrandao.expensetracker.domain.repository.CategoryRepository
import com.brunobrandao.expensetracker.domain.usecase.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val getTransactions: GetTransactionsUseCase,
    private val categoryRepository: CategoryRepository,
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observePreferences()
    }

    private fun observePreferences() {
        preferencesRepository.userPreferences.onEach { prefs ->
            _uiState.update {
                it.copy(
                    currency = prefs.currency,
                    themeMode = prefs.themeMode
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: SettingsEvent, onDeleteAccountSuccess: () -> Unit = {}) {
        when (event) {
            is SettingsEvent.CurrencyChanged -> {
                val userId = authRepository.currentUserId
                viewModelScope.launch {
                    preferencesRepository.updateCurrency(event.currency)
                    if (userId != null) {
                        preferencesRepository.pushCurrency(userId, event.currency)
                    }
                }
                _uiState.update { it.copy(showCurrencyDialog = false) }
            }
            is SettingsEvent.ThemeModeChanged -> {
                viewModelScope.launch {
                    preferencesRepository.updateThemeMode(event.themeMode)
                }
                _uiState.update { it.copy(showThemeDialog = false) }
            }
            is SettingsEvent.ToggleCurrencyDialog -> {
                _uiState.update { it.copy(showCurrencyDialog = !it.showCurrencyDialog) }
            }
            is SettingsEvent.ToggleThemeDialog -> {
                _uiState.update { it.copy(showThemeDialog = !it.showThemeDialog) }
            }
            is SettingsEvent.ToggleAboutDialog -> {
                _uiState.update { it.copy(showAboutDialog = !it.showAboutDialog) }
            }
            is SettingsEvent.ExportData -> {
                exportToCsv()
            }
            is SettingsEvent.DismissExportSuccess -> {
                _uiState.update { it.copy(showExportSuccess = false) }
            }
            is SettingsEvent.ToggleDeleteAccountDialog -> {
                _uiState.update {
                    it.copy(
                        showDeleteAccountDialog = !it.showDeleteAccountDialog,
                        deleteErrorMessage = null
                    )
                }
            }
            is SettingsEvent.DeleteAccount -> {
                deleteAccount(event.password, onDeleteAccountSuccess)
            }
        }
    }

    private fun deleteAccount(password: String, onSuccess: () -> Unit) {
        val userId = authRepository.currentUserId ?: return
        _uiState.update { it.copy(isDeleteLoading = true, deleteErrorMessage = null) }
        viewModelScope.launch {
            // Step 1: stop listeners before touching data
            syncRepository.stopSync()

            // Step 2: reauthenticate — MUST succeed before any data is touched
            authRepository.reauthenticate(password).onFailure { error ->
                _uiState.update {
                    it.copy(
                        isDeleteLoading = false,
                        deleteErrorMessage = error.message ?: "Incorrect password. Please try again."
                    )
                }
                return@launch
            }

            // Step 3: delete Firestore data (while auth is still valid)
            runCatching { syncRepository.deleteAllUserDataFromFirestore(userId) }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isDeleteLoading = false,
                        deleteErrorMessage = error.message ?: "Could not delete your data. Check your connection and try again."
                    )
                }
                return@launch
            }

            // Steps 4 & 5: clear local data and preferences (best-effort; local, should not fail)
            runCatching { syncRepository.deleteAllLocalData() }
            runCatching { preferencesRepository.clearAllPreferences() }

            // Step 6: delete Firebase Auth account
            authRepository.deleteAccount().onFailure { error ->
                _uiState.update {
                    it.copy(
                        isDeleteLoading = false,
                        deleteErrorMessage = error.message ?: "Could not delete account. Please try again."
                    )
                }
                return@launch
            }

            // Success — navigate to login
            _uiState.update { it.copy(isDeleteLoading = false, showDeleteAccountDialog = false) }
            onSuccess()
        }
    }

    private fun exportToCsv() {
        viewModelScope.launch {
            try {
                val transactions = getTransactions().first()
                val categoryNames = categoryRepository.observeCategories().first()
                    .associate { it.key to it.name }
                val fileDate = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

                val csvContent = buildCsvContent(transactions, categoryNames)

                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, "expense_tracker_$fileDate.csv")
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(csvContent.toByteArray())
                    }
                }

                _uiState.update { it.copy(showExportSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(showExportSuccess = false) }
            }
        }
    }
}

internal fun buildCsvContent(
    transactions: List<Transaction>,
    categoryNames: Map<String, String>
): String {
    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    return buildString {
        appendLine("Date,Type,Category,Description,Amount,Note")
        transactions.forEach { transaction ->
            val date = dateFormat.format(Date(transaction.date))
            val type = transaction.type.displayName
            val category = categoryNames[transaction.category] ?: transaction.category
            val description = "\"${transaction.description.replace("\"", "\"\"")}\""
            val amount = if (transaction.type == TransactionType.EXPENSE) {
                "-${transaction.amount}"
            } else {
                transaction.amount.toString()
            }
            val note = "\"${transaction.note.replace("\"", "\"\"")}\""
            appendLine("$date,$type,$category,$description,$amount,$note")
        }
    }
}
