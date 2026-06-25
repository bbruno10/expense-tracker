package com.brunobrandao.expensetracker.presentation.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brunobrandao.expensetracker.domain.model.RecurringFrequency
import com.brunobrandao.expensetracker.domain.model.TransactionType
import com.brunobrandao.expensetracker.ui.theme.LocalFinanceColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US).also {
    it.timeZone = TimeZone.getTimeZone("UTC")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditRecurringScreen(
    recurringId: Long,
    onNavigateBack: () -> Unit,
    viewModel: EditRecurringViewModel = hiltViewModel()
) {
    val focusManager = LocalFocusManager.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showNextDatePicker by remember { mutableStateOf(false) }
    val financeColors = LocalFinanceColors.current

    LaunchedEffect(recurringId) { viewModel.load(recurringId) }

    LaunchedEffect(state.isSaved) { if (state.isSaved) onNavigateBack() }
    LaunchedEffect(state.isDeleted) { if (state.isDeleted) onNavigateBack() }
    LaunchedEffect(state.notFound) { if (state.notFound) onNavigateBack() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onEvent(EditRecurringEvent.DismissError)
        }
    }

    if (showNextDatePicker && state.nextDueDate > 0L) {
        val nextDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.nextDueDate
        )
        DatePickerDialog(
            onDismissRequest = { showNextDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        nextDatePickerState.selectedDateMillis?.let { millis ->
                            viewModel.onEvent(EditRecurringEvent.NextDueDateChanged(millis))
                        }
                        showNextDatePicker = false
                    }
                ) { Text("Confirm", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showNextDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = nextDatePickerState) }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete rule", fontWeight = FontWeight.Bold) },
            text = { Text("This will delete the recurring rule. Transactions already generated will not be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.onEvent(EditRecurringEvent.Delete)
                }) { Text("Delete", color = financeColors.expense, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit recurring rule", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete rule", tint = financeColors.expense)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Type
            Text("Type", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TypeButton(
                    label = "Expense",
                    isSelected = state.type == TransactionType.EXPENSE,
                    color = financeColors.expense,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onEvent(EditRecurringEvent.TypeChanged(TransactionType.EXPENSE)) }
                )
                TypeButton(
                    label = "Income",
                    isSelected = state.type == TransactionType.INCOME,
                    color = financeColors.income,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onEvent(EditRecurringEvent.TypeChanged(TransactionType.INCOME)) }
                )
            }

            // Description
            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.onEvent(EditRecurringEvent.DescriptionChanged(it)) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary, cursorColor = MaterialTheme.colorScheme.primary)
            )

            // Amount
            OutlinedTextField(
                value = state.amount,
                onValueChange = { viewModel.onEvent(EditRecurringEvent.AmountChanged(it)) },
                label = { Text("Amount") },
                placeholder = { Text("0.00") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary, cursorColor = MaterialTheme.colorScheme.primary)
            )

            // Frequency
            Text("Frequency", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecurringFrequency.entries.forEach { freq ->
                    FilterChip(
                        selected = state.frequency == freq,
                        onClick = { viewModel.onEvent(EditRecurringEvent.FrequencyChanged(freq)) },
                        label = { Text(freq.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Category
            Text("Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.categories.forEach { category ->
                    FilterChip(
                        selected = state.category == category.key,
                        onClick = { viewModel.onEvent(EditRecurringEvent.CategoryChanged(category.key)) },
                        label = { Text("${category.icon} ${category.name}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Note
            OutlinedTextField(
                value = state.note,
                onValueChange = { viewModel.onEvent(EditRecurringEvent.NoteChanged(it)) },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary, cursorColor = MaterialTheme.colorScheme.primary)
            )

            // Active
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Active", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = state.active,
                    onCheckedChange = { viewModel.onEvent(EditRecurringEvent.ActiveChanged(it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                )
            }

            // Next due date (editable)
            if (state.nextDueDate > 0L) {
                Text("Next due date", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                        .clickable { showNextDatePicker = true }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateFormat.format(Date(state.nextDueDate)),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Select next due date", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.onEvent(EditRecurringEvent.Save) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.medium
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.padding(4.dp))
                } else {
                    Text("Save changes", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TypeButton(label: String, isSelected: Boolean, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(MaterialTheme.shapes.medium)
            .then(
                if (isSelected) Modifier.background(color.copy(alpha = 0.12f)).border(1.5.dp, color, MaterialTheme.shapes.medium)
                else Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow).border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
