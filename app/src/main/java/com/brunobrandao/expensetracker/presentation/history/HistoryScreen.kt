package com.brunobrandao.expensetracker.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brunobrandao.expensetracker.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brunobrandao.expensetracker.data.preferences.Currency
import com.brunobrandao.expensetracker.domain.model.Transaction
import com.brunobrandao.expensetracker.domain.model.TransactionType
import com.brunobrandao.expensetracker.presentation.home.TransactionDetailDialog
import com.brunobrandao.expensetracker.presentation.home.TransactionItem
import com.brunobrandao.expensetracker.presentation.util.CurrencyFormatter
import com.brunobrandao.expensetracker.ui.theme.CardGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val GreenPrimary = Color(0xFF1D9E75)
private val IncomeGreen = Color(0xFF43A047)
private val ExpenseRed = Color(0xFFE53935)

private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.US)

private data class MonthGroup(
    val label: String,
    val transactions: List<Transaction>,
    val totalIncome: Double,
    val totalExpense: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    onEditTransaction: (Long) -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val categoriesMap = remember(state.categories) { state.categories.associateBy { it.key } }
    var detailTransaction by remember { mutableStateOf<Transaction?>(null) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var pendingDeleteTransaction by remember { mutableStateOf<Transaction?>(null) }
    var pendingSwipeDeleteTransaction by remember { mutableStateOf<Transaction?>(null) }

    // Detail Dialog
    detailTransaction?.let { transaction ->
        TransactionDetailDialog(
            transaction = transaction,
            categoriesMap = categoriesMap,
            onDismiss = { detailTransaction = null },
            currency = state.currency
        )
    }

    // Edit/Delete Dialog
    selectedTransaction?.let { transaction ->
        AlertDialog(
            onDismissRequest = { selectedTransaction = null },
            title = { Text(transaction.description, fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.home_dialog_action_prompt)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTransaction = null
                        onEditTransaction(transaction.id)
                    }
                ) {
                    Text(stringResource(R.string.home_dialog_edit), color = CardGreen, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingDeleteTransaction = transaction
                        selectedTransaction = null
                    }
                ) {
                    Text(stringResource(R.string.home_dialog_delete), color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // Delete confirmation dialog (long press)
    pendingDeleteTransaction?.let { transaction ->
        AlertDialog(
            onDismissRequest = { pendingDeleteTransaction = null },
            title = { Text(stringResource(R.string.home_dialog_delete_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.home_dialog_delete_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(HistoryEvent.DeleteTransaction(transaction.id))
                        pendingDeleteTransaction = null
                    }
                ) {
                    Text(stringResource(R.string.home_dialog_confirm), color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTransaction = null }) {
                    Text(stringResource(R.string.home_dialog_cancel), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // Delete confirmation dialog (swipe)
    pendingSwipeDeleteTransaction?.let { transaction ->
        AlertDialog(
            onDismissRequest = { pendingSwipeDeleteTransaction = null },
            title = { Text(stringResource(R.string.home_dialog_delete_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.home_dialog_delete_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(HistoryEvent.DeleteTransaction(transaction.id))
                        pendingSwipeDeleteTransaction = null
                    }
                ) {
                    Text(stringResource(R.string.home_dialog_confirm), color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSwipeDeleteTransaction = null }) {
                    Text(stringResource(R.string.home_dialog_cancel), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // Group transactions by month
    val monthGroups = remember(state.transactions) {
        state.transactions
            .groupBy { transaction ->
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = transaction.date
                }
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                year * 100 + month
            }
            .toSortedMap(compareByDescending { it })
            .map { (_, transactions) ->
                val label = monthYearFormat.format(Date(transactions.first().date))
                    .replaceFirstChar { it.uppercase() }
                val totalIncome = transactions
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf { it.amount }
                val totalExpense = transactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }
                MonthGroup(label, transactions, totalIncome, totalExpense)
            }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Title
        Text(
            text = stringResource(R.string.history_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // Category filter chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // All chip
            item {
                FilterChip(
                    selected = state.filterType == null && state.filterCategory == null,
                    onClick = {
                        viewModel.onEvent(HistoryEvent.FilterByType(null))
                        viewModel.onEvent(HistoryEvent.FilterByCategory(null))
                    },
                    label = { Text(stringResource(R.string.history_filter_all), style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GreenPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }

            // Category chips
            items(state.categories) { category ->
                FilterChip(
                    selected = state.filterCategory == category.key,
                    onClick = {
                        viewModel.onEvent(HistoryEvent.FilterByType(null))
                        if (state.filterCategory == category.key) {
                            viewModel.onEvent(HistoryEvent.FilterByCategory(null))
                        } else {
                            viewModel.onEvent(HistoryEvent.FilterByCategory(category.key))
                        }
                    },
                    label = {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = category.color,
                        selectedLabelColor = Color.White,
                        containerColor = category.lightColor,
                        labelColor = category.color
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GreenPrimary)
            }
        } else if (state.transactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                monthGroups.forEach { monthGroup ->
                    // Month Header
                    item(key = "header_${monthGroup.label}") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 4.dp)
                        ) {
                            Text(
                                text = monthGroup.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (monthGroup.totalIncome > 0) {
                                    Text(
                                        text = "+ ${CurrencyFormatter.format(monthGroup.totalIncome, state.currency)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = IncomeGreen
                                    )
                                }
                                if (monthGroup.totalExpense > 0) {
                                    Text(
                                        text = "- ${CurrencyFormatter.format(monthGroup.totalExpense, state.currency)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = ExpenseRed
                                    )
                                }
                            }
                        }
                    }

                    // Transactions
                    items(
                        items = monthGroup.transactions,
                        key = { it.id }
                    ) { transaction ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                    pendingSwipeDeleteTransaction = transaction
                                }
                                false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        text = stringResource(R.string.home_dialog_delete),
                                        color = ExpenseRed,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(end = 16.dp)
                                    )
                                }
                            },
                            enableDismissFromStartToEnd = false
                        ) {
                            TransactionItem(
                                transaction = transaction,
                                categoriesMap = categoriesMap,
                                onClick = { detailTransaction = transaction },
                                onLongClick = { selectedTransaction = transaction },
                                currency = state.currency
                            )
                        }
                    }
                }

                // Swipe hint
                item {
                    Text(
                        text = stringResource(R.string.history_swipe_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}
