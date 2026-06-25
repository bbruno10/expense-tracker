package com.brunobrandao.expensetracker.presentation.add

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.brunobrandao.expensetracker.presentation.categories.CategoryFormDialog
import com.brunobrandao.expensetracker.ui.theme.LocalFinanceColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
private val recurringDateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US).also {
    it.timeZone = TimeZone.getTimeZone("UTC")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(
    onNavigateBack: () -> Unit,
    transactionId: Long = 0L,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val focusManager = LocalFocusManager.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    val financeColors = LocalFinanceColors.current

    LaunchedEffect(transactionId) {
        if (transactionId > 0L) {
            viewModel.loadTransaction(transactionId)
        }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onNavigateBack()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onEvent(AddTransactionEvent.DismissError)
        }
    }

    if (showStartDatePicker) {
        val startDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.repeatStartDate
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        startDatePickerState.selectedDateMillis?.let { millis ->
                            viewModel.onEvent(AddTransactionEvent.StartDateChanged(millis))
                        }
                        showStartDatePicker = false
                    }
                ) { Text("Confirm", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = startDatePickerState) }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.onEvent(AddTransactionEvent.DateChanged(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirm", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    state.newCategoryForm?.let { form ->
        CategoryFormDialog(
            title = "New category",
            name = form.name,
            icon = form.icon,
            colorIndex = form.colorIndex,
            nameError = form.nameError,
            onNameChange = { viewModel.onEvent(AddTransactionEvent.NewCategoryNameChanged(it)) },
            onIconChange = { viewModel.onEvent(AddTransactionEvent.NewCategoryIconChanged(it)) },
            onColorChange = { viewModel.onEvent(AddTransactionEvent.NewCategoryColorChanged(it)) },
            onConfirm = { viewModel.onEvent(AddTransactionEvent.SaveNewCategory) },
            onDismiss = { viewModel.onEvent(AddTransactionEvent.DismissNewCategoryDialog) }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isEditing) "Edit Transaction" else "New Transaction",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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

            Text(
                text = "Type",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TypeButton(
                    label = "Expense",
                    isSelected = state.type == TransactionType.EXPENSE,
                    color = financeColors.expense,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.onEvent(AddTransactionEvent.TypeChanged(TransactionType.EXPENSE))
                    }
                )
                TypeButton(
                    label = "Income",
                    isSelected = state.type == TransactionType.INCOME,
                    color = financeColors.income,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.onEvent(AddTransactionEvent.TypeChanged(TransactionType.INCOME))
                    }
                )
            }

            OutlinedTextField(
                value = state.description,
                onValueChange = {
                    viewModel.onEvent(AddTransactionEvent.DescriptionChanged(it))
                },
                label = { Text("Description") },
                placeholder = { Text("e.g. Lunch, Salary...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            OutlinedTextField(
                value = state.amount,
                onValueChange = {
                    viewModel.onEvent(AddTransactionEvent.AmountChanged(it))
                },
                label = { Text("Amount") },
                placeholder = { Text("0.00") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            if (!state.repeatEnabled) {
                Text(
                    text = "Date",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            MaterialTheme.shapes.medium
                        )
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateFormat.format(Date(state.date)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "Select date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Text(
                text = "Category",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.categories.forEach { category ->
                    FilterChip(
                        selected = state.category == category.key,
                        onClick = {
                            viewModel.onEvent(AddTransactionEvent.CategoryChanged(category.key))
                        },
                        label = { Text("${category.icon} ${category.name}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = { viewModel.onEvent(AddTransactionEvent.ShowNewCategoryDialog) },
                    label = { Text("+ New") },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            OutlinedTextField(
                value = state.note,
                onValueChange = {
                    viewModel.onEvent(AddTransactionEvent.NoteChanged(it))
                },
                label = { Text("Note (optional)") },
                placeholder = { Text("Additional details...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            if (transactionId == 0L) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Repeat",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Create this as a recurring rule",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.repeatEnabled,
                        onCheckedChange = { viewModel.onEvent(AddTransactionEvent.RepeatToggled) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                    )
                }

                if (state.repeatEnabled) {
                    Text(
                        text = "Frequency",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RecurringFrequency.entries.forEach { freq ->
                            FilterChip(
                                selected = state.repeatFrequency == freq,
                                onClick = { viewModel.onEvent(AddTransactionEvent.FrequencyChanged(freq)) },
                                label = {
                                    Text(freq.name.lowercase().replaceFirstChar { it.uppercase() })
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Text(
                        text = "Start date",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                            .clickable { showStartDatePicker = true }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = recurringDateFormat.format(Date(state.repeatStartDate)),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Select start date", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.onEvent(AddTransactionEvent.Save) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.medium
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.padding(4.dp)
                    )
                } else {
                    Text(
                        text = if (state.isEditing) "Update Transaction" else "Save Transaction",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TypeButton(
    label: String,
    isSelected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(MaterialTheme.shapes.medium)
            .then(
                if (isSelected) {
                    Modifier
                        .background(color.copy(alpha = 0.12f))
                        .border(1.5.dp, color, MaterialTheme.shapes.medium)
                } else {
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            MaterialTheme.shapes.medium
                        )
                }
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
