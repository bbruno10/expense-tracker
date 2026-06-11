package com.brunobrandao.expensetracker.presentation.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brunobrandao.expensetracker.data.sync.SyncRepository
import com.brunobrandao.expensetracker.domain.model.RecurringTransaction
import com.brunobrandao.expensetracker.domain.repository.AuthRepository
import com.brunobrandao.expensetracker.domain.repository.RecurringTransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val recurringRepository: RecurringTransactionRepository,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val rules: StateFlow<List<RecurringTransaction>> = recurringRepository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun setActive(id: Long, active: Boolean) {
        viewModelScope.launch {
            recurringRepository.setActive(id, active)
            authRepository.currentUserId?.let { userId ->
                try { syncRepository.syncWriteRecurring(id, userId) } catch (_: Exception) {}
            }
        }
    }

    fun deleteById(id: Long) {
        viewModelScope.launch {
            val userId = authRepository.currentUserId
            if (userId != null) {
                try { syncRepository.syncDeleteRecurring(id, userId) } catch (_: Exception) {
                    recurringRepository.deleteById(id)
                }
            } else {
                recurringRepository.deleteById(id)
            }
        }
    }
}
