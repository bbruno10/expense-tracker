package com.brunobrandao.expensetracker.presentation.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brunobrandao.expensetracker.data.sync.SyncRepository
import com.brunobrandao.expensetracker.domain.model.RecurringTransaction
import com.brunobrandao.expensetracker.domain.model.Transaction
import com.brunobrandao.expensetracker.domain.repository.AuthRepository
import com.brunobrandao.expensetracker.domain.repository.CategoryRepository
import com.brunobrandao.expensetracker.domain.repository.RecurringTransactionRepository
import com.brunobrandao.expensetracker.domain.repository.TransactionRepository
import com.brunobrandao.expensetracker.domain.usecase.AddTransactionUseCase
import com.brunobrandao.expensetracker.domain.usecase.CreateCategoryUseCase
import com.brunobrandao.expensetracker.domain.usecase.GenerateDueRecurringTransactionsUseCase
import com.brunobrandao.expensetracker.domain.util.Clock
import com.brunobrandao.expensetracker.presentation.categories.CATEGORY_COLORS
import com.brunobrandao.expensetracker.presentation.categories.CategoryFormState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val addTransaction: AddTransactionUseCase,
    private val repository: TransactionRepository,
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    private val recurringRepository: RecurringTransactionRepository,
    private val generateDue: GenerateDueRecurringTransactionsUseCase,
    private val categoryRepository: CategoryRepository,
    private val createCategory: CreateCategoryUseCase,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    init {
        categoryRepository.observeCategories().onEach { cats ->
            _uiState.update { it.copy(categories = cats) }
        }.launchIn(viewModelScope)
    }

    fun loadTransaction(transactionId: Long) {
        if (transactionId <= 0L) return
        viewModelScope.launch {
            repository.getTransactionById(transactionId)?.let { transaction ->
                _uiState.update {
                    it.copy(
                        editingId = transaction.id,
                        description = transaction.description,
                        amount = transaction.amount.toString(),
                        type = transaction.type,
                        category = transaction.category,
                        date = transaction.date,
                        note = transaction.note,
                        recurringId = transaction.recurringId
                    )
                }
            }
        }
    }

    fun onEvent(event: AddTransactionEvent) {
        when (event) {
            is AddTransactionEvent.DescriptionChanged ->
                _uiState.update { it.copy(description = event.value) }
            is AddTransactionEvent.AmountChanged -> {
                val filtered = event.value.filter { c -> c.isDigit() || c == '.' }
                _uiState.update { it.copy(amount = filtered) }
            }
            is AddTransactionEvent.TypeChanged ->
                _uiState.update { it.copy(type = event.value) }
            is AddTransactionEvent.CategoryChanged ->
                _uiState.update { it.copy(category = event.value) }
            is AddTransactionEvent.DateChanged ->
                _uiState.update { it.copy(date = event.value) }
            is AddTransactionEvent.NoteChanged ->
                _uiState.update { it.copy(note = event.value) }
            is AddTransactionEvent.RepeatToggled ->
                _uiState.update { it.copy(repeatEnabled = !it.repeatEnabled) }
            is AddTransactionEvent.FrequencyChanged ->
                _uiState.update { it.copy(repeatFrequency = event.value) }
            is AddTransactionEvent.StartDateChanged ->
                _uiState.update { it.copy(repeatStartDate = event.value) }
            is AddTransactionEvent.Save -> saveTransaction()
            is AddTransactionEvent.DismissError ->
                _uiState.update { it.copy(errorMessage = null) }
            is AddTransactionEvent.ShowNewCategoryDialog ->
                _uiState.update { it.copy(newCategoryForm = CategoryFormState()) }
            is AddTransactionEvent.DismissNewCategoryDialog ->
                _uiState.update { it.copy(newCategoryForm = null) }
            is AddTransactionEvent.NewCategoryNameChanged ->
                _uiState.update { it.copy(newCategoryForm = it.newCategoryForm?.copy(name = event.value, nameError = null)) }
            is AddTransactionEvent.NewCategoryIconChanged ->
                _uiState.update { it.copy(newCategoryForm = it.newCategoryForm?.copy(icon = event.value)) }
            is AddTransactionEvent.NewCategoryColorChanged ->
                _uiState.update { it.copy(newCategoryForm = it.newCategoryForm?.copy(colorIndex = event.value)) }
            is AddTransactionEvent.SaveNewCategory -> saveNewCategory()
        }
    }

    private fun saveNewCategory() {
        val form = _uiState.value.newCategoryForm ?: return
        val colorOption = CATEGORY_COLORS.getOrElse(form.colorIndex) { CATEGORY_COLORS[0] }
        viewModelScope.launch {
            runCatching {
                createCategory(form.name, form.icon, colorOption.color, colorOption.lightColor)
            }.onSuccess { category ->
                _uiState.update { it.copy(newCategoryForm = null, category = category.key) }
            }.onFailure { e ->
                when (e) {
                    is IllegalArgumentException ->
                        _uiState.update { it.copy(newCategoryForm = it.newCategoryForm?.copy(nameError = e.message)) }
                    else ->
                        _uiState.update { it.copy(newCategoryForm = null, errorMessage = e.message) }
                }
            }
        }
    }

    private fun saveTransaction() {
        val state = _uiState.value

        if (state.description.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a description") }
            return
        }
        val amount = state.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid amount") }
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                if (state.repeatEnabled && !state.isEditing) {
                    val now = clock.now()
                    val rule = RecurringTransaction(
                        description = state.description.trim(),
                        amount = amount,
                        type = state.type,
                        category = state.category,
                        note = state.note.trim(),
                        frequency = state.repeatFrequency,
                        startDate = state.repeatStartDate,
                        nextDueDate = state.repeatStartDate,
                        active = true
                    )
                    val insertedId = recurringRepository.upsert(rule)
                    generateDue(now)
                    authRepository.currentUserId?.let { userId ->
                        try { syncRepository.syncWriteRecurring(insertedId, userId) } catch (_: Exception) {}
                    }
                } else {
                    val transaction = Transaction(
                        id = state.editingId ?: 0,
                        description = state.description.trim(),
                        amount = amount,
                        type = state.type,
                        category = state.category,
                        date = state.date,
                        note = state.note.trim(),
                        recurringId = state.recurringId
                    )

                    val localId: Long = if (state.isEditing) {
                        repository.updateTransaction(transaction)
                        transaction.id
                    } else {
                        addTransaction(transaction)
                    }

                    authRepository.currentUserId?.let { userId ->
                        try { syncRepository.syncWrite(localId, userId) } catch (_: Exception) {}
                    }
                }
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to save: ${e.message}")
                }
            }
        }
    }
}
