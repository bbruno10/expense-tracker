package com.brunobrandao.expensetracker.presentation.categories

import com.brunobrandao.expensetracker.R
import com.brunobrandao.expensetracker.ui.components.EmptyStateView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brunobrandao.expensetracker.domain.model.Category

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManageCategoriesScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManageCategoriesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(ManageCategoriesEvent.DismissSnackbar)
        }
    }

    // ── Category form dialog (create / edit) ──────────────────────────────────
    state.form?.let { form ->
        CategoryFormDialog(
            title = if (form.editingKey == null) "New category" else "Edit category",
            name = form.name,
            icon = form.icon,
            colorIndex = form.colorIndex,
            nameError = form.nameError,
            onNameChange = { viewModel.onEvent(ManageCategoriesEvent.FormNameChanged(it)) },
            onIconChange = { viewModel.onEvent(ManageCategoriesEvent.FormIconChanged(it)) },
            onColorChange = { viewModel.onEvent(ManageCategoriesEvent.FormColorChanged(it)) },
            onConfirm = { viewModel.onEvent(ManageCategoriesEvent.SaveForm) },
            onDismiss = { viewModel.onEvent(ManageCategoriesEvent.DismissDialog) }
        )
    }

    // ── Delete confirmation dialog ─────────────────────────────────────────────
    if (state.pendingDeleteKey != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(ManageCategoriesEvent.DismissDialog) },
            title = { Text("Delete category", fontWeight = FontWeight.Bold) },
            text = { Text("This category will be permanently deleted. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onEvent(ManageCategoriesEvent.ConfirmDelete) }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(ManageCategoriesEvent.DismissDialog) }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Manage categories", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!state.showArchived) {
                FloatingActionButton(
                    onClick = { viewModel.onEvent(ManageCategoriesEvent.ShowCreateDialog) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add category")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Active / Archived toggle ──────────────────────────────────────
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !state.showArchived,
                    onClick = { if (state.showArchived) viewModel.onEvent(ManageCategoriesEvent.ToggleShowArchived) },
                    label = { Text("Active") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = state.showArchived,
                    onClick = { if (!state.showArchived) viewModel.onEvent(ManageCategoriesEvent.ToggleShowArchived) },
                    label = { Text("Archived (${state.archivedCategories.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }

            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                !state.showArchived && state.activeCategories.isEmpty() -> {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        EmptyStateView(
                            animationRes = R.raw.lottie_empty_generic,
                            message = "No categories yet\nTap + to create your first custom category",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp, horizontal = 32.dp)
                        )
                    }
                }

                state.showArchived && state.archivedCategories.isEmpty() -> {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        EmptyStateView(
                            animationRes = R.raw.lottie_empty_generic,
                            message = "No archived categories",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp, horizontal = 32.dp)
                        )
                    }
                }

                else -> {
                    val list = if (state.showArchived) state.archivedCategories else state.activeCategories
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { Spacer(Modifier.height(4.dp)) }
                        items(list, key = { it.key }) { category ->
                            if (state.showArchived) {
                                ArchivedCategoryItem(
                                    category = category,
                                    onUnarchive = { viewModel.onEvent(ManageCategoriesEvent.Unarchive(category.key)) }
                                )
                            } else {
                                ActiveCategoryItem(
                                    category = category,
                                    onEdit = { viewModel.onEvent(ManageCategoriesEvent.ShowEditDialog(category)) },
                                    onArchive = { viewModel.onEvent(ManageCategoriesEvent.Archive(category.key)) },
                                    onDelete = { viewModel.onEvent(ManageCategoriesEvent.ShowDeleteConfirm(category.key)) }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

// ── Category list items ───────────────────────────────────────────────────────

@Composable
private fun ActiveCategoryItem(
    category: Category,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Coloured icon badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(category.lightColor),
                contentAlignment = Alignment.Center
            ) {
                Text(category.icon, style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (category.isDefault) {
                        Spacer(Modifier.width(8.dp))
                        DefaultBadge()
                    }
                }
            }

            // Three-dot menu
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { menuExpanded = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Archive") },
                        onClick = { menuExpanded = false; onArchive() }
                    )
                    if (!category.isDefault) {
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = { menuExpanded = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchivedCategoryItem(
    category: Category,
    onUnarchive: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(category.lightColor.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    category.icon,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (category.isDefault) {
                        Spacer(Modifier.width(8.dp))
                        DefaultBadge()
                    }
                }
            }

            TextButton(onClick = onUnarchive) {
                Text("Unarchive", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DefaultBadge() {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "default",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// CategoryFormDialog lives in CategoryFormDialog.kt and is shared with AddTransactionScreen.
