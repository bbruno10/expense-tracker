package com.brunobrandao.expensetracker.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brunobrandao.expensetracker.domain.repository.CategoryRepository
import com.brunobrandao.expensetracker.domain.usecase.ArchiveCategoryUseCase
import com.brunobrandao.expensetracker.domain.usecase.CreateCategoryUseCase
import com.brunobrandao.expensetracker.domain.usecase.DeleteCategoryUseCase
import com.brunobrandao.expensetracker.domain.usecase.UnarchiveCategoryUseCase
import com.brunobrandao.expensetracker.domain.usecase.UpdateCategoryUseCase
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
class ManageCategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val createCategory: CreateCategoryUseCase,
    private val updateCategory: UpdateCategoryUseCase,
    private val archiveCategory: ArchiveCategoryUseCase,
    private val unarchiveCategory: UnarchiveCategoryUseCase,
    private val deleteCategory: DeleteCategoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageCategoriesUiState())
    val uiState: StateFlow<ManageCategoriesUiState> = _uiState.asStateFlow()

    init {
        categoryRepository.observeCategories().onEach { all ->
            _uiState.update {
                it.copy(
                    activeCategories = all.filter { cat -> !cat.archived },
                    archivedCategories = all.filter { cat -> cat.archived },
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: ManageCategoriesEvent) {
        when (event) {
            ManageCategoriesEvent.ToggleShowArchived ->
                _uiState.update { it.copy(showArchived = !it.showArchived) }

            ManageCategoriesEvent.ShowCreateDialog ->
                _uiState.update { it.copy(form = CategoryFormState()) }

            is ManageCategoriesEvent.ShowEditDialog -> {
                val cat = event.category
                val colorIndex = CATEGORY_COLORS.indexOfFirst { it.color == cat.color }
                    .takeIf { it >= 0 } ?: 0
                _uiState.update {
                    it.copy(
                        form = CategoryFormState(
                            editingKey = cat.key,
                            name = cat.name,
                            icon = cat.icon,
                            colorIndex = colorIndex
                        )
                    )
                }
            }

            is ManageCategoriesEvent.ShowDeleteConfirm ->
                _uiState.update { it.copy(pendingDeleteKey = event.key) }

            ManageCategoriesEvent.DismissDialog ->
                _uiState.update { it.copy(form = null, pendingDeleteKey = null) }

            is ManageCategoriesEvent.FormNameChanged ->
                _uiState.update { it.copy(form = it.form?.copy(name = event.name, nameError = null)) }

            is ManageCategoriesEvent.FormIconChanged ->
                _uiState.update { it.copy(form = it.form?.copy(icon = event.icon)) }

            is ManageCategoriesEvent.FormColorChanged ->
                _uiState.update { it.copy(form = it.form?.copy(colorIndex = event.colorIndex)) }

            ManageCategoriesEvent.SaveForm -> saveForm()

            is ManageCategoriesEvent.Archive -> viewModelScope.launch {
                runCatching { archiveCategory(event.key) }
                    .onFailure { e -> _uiState.update { it.copy(snackbarMessage = e.message) } }
            }

            is ManageCategoriesEvent.Unarchive -> viewModelScope.launch {
                runCatching { unarchiveCategory(event.key) }
                    .onFailure { e -> _uiState.update { it.copy(snackbarMessage = e.message) } }
            }

            ManageCategoriesEvent.ConfirmDelete -> {
                val key = _uiState.value.pendingDeleteKey ?: return
                _uiState.update { it.copy(pendingDeleteKey = null) }
                viewModelScope.launch {
                    runCatching { deleteCategory(key) }
                        .onFailure { e -> _uiState.update { it.copy(snackbarMessage = e.message) } }
                }
            }

            ManageCategoriesEvent.DismissSnackbar ->
                _uiState.update { it.copy(snackbarMessage = null) }
        }
    }

    private fun saveForm() {
        val form = _uiState.value.form ?: return
        val colorOption = CATEGORY_COLORS.getOrElse(form.colorIndex) { CATEGORY_COLORS[0] }

        viewModelScope.launch {
            if (form.editingKey == null) {
                // Create
                runCatching {
                    createCategory(form.name, form.icon, colorOption.color, colorOption.lightColor)
                }.onSuccess {
                    _uiState.update { it.copy(form = null) }
                }.onFailure { e ->
                    when (e) {
                        is IllegalArgumentException ->
                            _uiState.update { it.copy(form = it.form?.copy(nameError = e.message)) }
                        else ->
                            _uiState.update { it.copy(form = null, snackbarMessage = e.message) }
                    }
                }
            } else {
                // Update
                runCatching {
                    updateCategory(form.editingKey, form.name, form.icon, colorOption.color, colorOption.lightColor)
                }.onSuccess {
                    _uiState.update { it.copy(form = null) }
                }.onFailure { e ->
                    when (e) {
                        is IllegalArgumentException ->
                            _uiState.update { it.copy(form = it.form?.copy(nameError = e.message)) }
                        else ->
                            _uiState.update { it.copy(form = null, snackbarMessage = e.message) }
                    }
                }
            }
        }
    }
}
