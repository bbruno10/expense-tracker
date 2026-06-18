package com.brunobrandao.expensetracker.presentation.categories

import androidx.compose.ui.graphics.Color
import com.brunobrandao.expensetracker.domain.model.Category

// ── Curated palette — 12 colour pairs (primary + light background) ────────────
data class CategoryColorOption(val color: Color, val lightColor: Color)

val CATEGORY_COLORS: List<CategoryColorOption> = listOf(
    CategoryColorOption(Color(0xFFD85A30), Color(0xFFFAECE7)), // Orange
    CategoryColorOption(Color(0xFF378ADD), Color(0xFFE6F1FB)), // Blue
    CategoryColorOption(Color(0xFF2A9D8F), Color(0xFFCCEFEB)), // Teal
    CategoryColorOption(Color(0xFFE24B4A), Color(0xFFFCEBEB)), // Red
    CategoryColorOption(Color(0xFF5C6BC0), Color(0xFFE8EAF6)), // Indigo
    CategoryColorOption(Color(0xFF7F77DD), Color(0xFFEEEDFE)), // Purple
    CategoryColorOption(Color(0xFFD4537E), Color(0xFFFBEAF0)), // Pink
    CategoryColorOption(Color(0xFFBA7517), Color(0xFFFAEEDA)), // Amber
    CategoryColorOption(Color(0xFF1D9E75), Color(0xFFE1F5EE)), // Green (brand)
    CategoryColorOption(Color(0xFF5DCAA5), Color(0xFFDFF5ED)), // Mint
    CategoryColorOption(Color(0xFF78909C), Color(0xFFECEFF1)), // Steel
    CategoryColorOption(Color(0xFF795548), Color(0xFFEFEBE9)), // Brown
)

// ── Curated icon set — emoji strings ─────────────────────────────────────────
val CATEGORY_ICONS: List<String> = listOf(
    "🍔", "🍕", "🍣", "🛒", "☕",
    "🚗", "✈️", "🚌", "⛽", "🚲",
    "💊", "🏥", "🏋️", "🧘", "🩺",
    "🏠", "🔌", "🛋️", "🔧", "🌿",
    "🎮", "🎬", "🎵", "📚", "🎨",
    "💰", "💳", "📈", "💼", "🏦",
    "🐾", "👶", "🎁", "📱", "✂️",
    "📌",
)

// ── Form state (shared between create & edit dialogs) ─────────────────────────
data class CategoryFormState(
    val editingKey: String? = null,   // null → create, non-null → edit
    val name: String = "",
    val icon: String = "📌",
    val colorIndex: Int = 8,          // default → Green (brand colour)
    val nameError: String? = null
)

// ── Screen state ──────────────────────────────────────────────────────────────
data class ManageCategoriesUiState(
    val activeCategories: List<Category> = emptyList(),
    val archivedCategories: List<Category> = emptyList(),
    val showArchived: Boolean = false,
    val isLoading: Boolean = true,
    val form: CategoryFormState? = null,       // null → dialog closed
    val pendingDeleteKey: String? = null,       // non-null → confirm-delete dialog open
    val snackbarMessage: String? = null
)

// ── Events ────────────────────────────────────────────────────────────────────
sealed interface ManageCategoriesEvent {
    data object ToggleShowArchived : ManageCategoriesEvent
    data object ShowCreateDialog : ManageCategoriesEvent
    data class ShowEditDialog(val category: Category) : ManageCategoriesEvent
    data class ShowDeleteConfirm(val key: String) : ManageCategoriesEvent
    data object DismissDialog : ManageCategoriesEvent
    data class FormNameChanged(val name: String) : ManageCategoriesEvent
    data class FormIconChanged(val icon: String) : ManageCategoriesEvent
    data class FormColorChanged(val colorIndex: Int) : ManageCategoriesEvent
    data object SaveForm : ManageCategoriesEvent
    data class Archive(val key: String) : ManageCategoriesEvent
    data class Unarchive(val key: String) : ManageCategoriesEvent
    data object ConfirmDelete : ManageCategoriesEvent
    data object DismissSnackbar : ManageCategoriesEvent
}
