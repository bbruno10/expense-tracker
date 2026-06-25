package com.brunobrandao.expensetracker.presentation.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.brunobrandao.expensetracker.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brunobrandao.expensetracker.data.preferences.Currency
import com.brunobrandao.expensetracker.domain.model.Category
import com.brunobrandao.expensetracker.domain.model.DefaultCategories
import com.brunobrandao.expensetracker.domain.model.Transaction
import com.brunobrandao.expensetracker.domain.model.TransactionType
import com.brunobrandao.expensetracker.presentation.util.CurrencyFormatter
import com.brunobrandao.expensetracker.ui.theme.CardGreen
import com.brunobrandao.expensetracker.ui.theme.CardGreenDark
import com.brunobrandao.expensetracker.ui.theme.CardRed
import com.brunobrandao.expensetracker.ui.theme.CardRedDark
import com.brunobrandao.expensetracker.ui.theme.LocalFinanceColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@Composable
fun HomeScreen(
    onNavigateToHistory: () -> Unit,
    onEditTransaction: (Long) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isNegative = state.balance < 0
    val financeColors = LocalFinanceColors.current
    var detailTransaction by remember { mutableStateOf<Transaction?>(null) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var pendingDeleteTransaction by remember { mutableStateOf<Transaction?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    // Detail Dialog
    detailTransaction?.let { transaction ->
        TransactionDetailDialog(
            transaction = transaction,
            categoriesMap = state.categoriesMap,
            onDismiss = { detailTransaction = null },
            currency = state.currency
        )
    }

    // Edit/Delete Dialog
    selectedTransaction?.let { transaction ->
        AlertDialog(
            onDismissRequest = { selectedTransaction = null },
            title = {
                Text(
                    text = transaction.description,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.home_dialog_action_prompt))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTransaction = null
                        onEditTransaction(transaction.id)
                    }
                ) {
                    Text(stringResource(R.string.home_dialog_edit), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingDeleteTransaction = transaction
                        selectedTransaction = null
                    }
                ) {
                    Text(stringResource(R.string.home_dialog_delete), color = financeColors.expense, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // Delete confirmation dialog
    pendingDeleteTransaction?.let { transaction ->
        AlertDialog(
            onDismissRequest = { pendingDeleteTransaction = null },
            title = { Text(stringResource(R.string.home_dialog_delete_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.home_dialog_delete_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onDeleteTransaction(transaction.id)
                        pendingDeleteTransaction = null
                    }
                ) {
                    Text(stringResource(R.string.home_dialog_confirm), color = financeColors.expense, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTransaction = null }) {
                    Text(stringResource(R.string.home_dialog_cancel), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Header
            item {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Balance Card
            item {
                BalanceCard(
                    balance = state.balance,
                    income = state.totalIncome,
                    expense = state.totalExpense,
                    isNegative = isNegative,
                    currency = state.currency
                )
            }

            // Period Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    TimePeriod.entries.forEach { period ->
                        FilterChip(
                            selected = state.selectedPeriod == period,
                            onClick = { viewModel.onPeriodChanged(period) },
                            label = { Text(period.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Period Navigator
            item {
                PeriodNavigator(
                    label = state.periodLabel,
                    canGoNext = state.canGoNext,
                    onPrevious = { viewModel.onPreviousPeriod() },
                    onNext = { viewModel.onNextPeriod() }
                )
            }

            // Alert banner when negative
            if (isNegative) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = financeColors.expense.copy(alpha = 0.08f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = financeColors.expense,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.home_negative_balance_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = financeColors.expense
                            )
                        }
                    }
                }
            }

            // Recent Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.home_recent_transactions),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.home_see_all),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onNavigateToHistory() }
                    )
                }
            }

            // Transaction List
            item {
                AnimatedVisibility(
                    visible = state.recentTransactions.isEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.home_empty_transactions),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            items(state.recentTransactions, key = { it.id }) { transaction ->
                TransactionItem(
                    transaction = transaction,
                    categoriesMap = state.categoriesMap,
                    onClick = { detailTransaction = transaction },
                    onLongClick = { selectedTransaction = transaction },
                    currency = state.currency
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun PeriodNavigator(
    label: String,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.home_previous_period),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        IconButton(
            onClick = onNext,
            enabled = canGoNext
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.home_next_period),
                tint = if (canGoNext) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun BalanceCard(
    balance: Double,
    income: Double,
    expense: Double,
    isNegative: Boolean,
    currency: Currency
) {

    val gradientStart by animateColorAsState(
        targetValue = if (isNegative) CardRed else CardGreen,
        animationSpec = tween(durationMillis = 600),
        label = "card_start"
    )
    val gradientEnd by animateColorAsState(
        targetValue = if (isNegative) CardRedDark else CardGreenDark,
        animationSpec = tween(durationMillis = 600),
        label = "card_end"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(gradientStart, gradientEnd)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.home_total_balance),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Icon(
                        imageVector = if (isNegative) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                        contentDescription = stringResource(R.string.cd_balance_trend),
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = CurrencyFormatter.format(balance, currency),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Income box
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.15f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = stringResource(R.string.home_income),
                                    tint = Color(0xFF90EE90),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.home_income),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = CurrencyFormatter.format(income, currency),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    // Expense box
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.15f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = stringResource(R.string.home_expenses),
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.home_expenses),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = CurrencyFormatter.format(expense, currency),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    categoriesMap: Map<String, Category> = emptyMap(),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    currency: Currency = Currency.USD
) {
    val category = categoriesMap[transaction.category] ?: DefaultCategories.fallback(transaction.category)
    val formatter = remember { DateTimeFormatter.ofPattern("MM/dd/yyyy") }
    val financeColors = LocalFinanceColors.current
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null || onLongClick != null)
                    Modifier.combinedClickable(
                        onClick = { onClick?.invoke() },
                        onLongClick = onLongClick
                    )
                else Modifier
            ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            if (transaction.type == TransactionType.INCOME)
                                financeColors.income.copy(alpha = 0.1f)
                            else
                                financeColors.expense.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.icon,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = transaction.description,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (transaction.recurringId != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = "Recurring",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Text(
                        text = "${category.name} • ${Instant.ofEpochMilli(transaction.date).atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "${if (transaction.type == TransactionType.INCOME) "+" else "-"} ${CurrencyFormatter.format(transaction.amount, currency)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (transaction.type == TransactionType.INCOME) financeColors.income else financeColors.expense
            )
        }
    }
}

@Composable
fun TransactionDetailDialog(
    transaction: Transaction,
    categoriesMap: Map<String, Category> = emptyMap(),
    onDismiss: () -> Unit,
    currency: Currency = Currency.USD
) {
    val category = categoriesMap[transaction.category] ?: DefaultCategories.fallback(transaction.category)
    val formatter = remember { DateTimeFormatter.ofPattern("MM/dd/yyyy") }
    val dateStr = Instant.ofEpochMilli(transaction.date)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(formatter)
    val isIncome = transaction.type == TransactionType.INCOME
    val financeColors = LocalFinanceColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(category.icon, style = MaterialTheme.typography.titleLarge)
                Text(transaction.description, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${if (isIncome) "+" else "-"} ${CurrencyFormatter.format(transaction.amount, currency)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) financeColors.income else financeColors.expense
                )
                DetailRow(label = "Category", value = "${category.icon} ${category.name}")
                DetailRow(label = "Date", value = dateStr)
                DetailRow(label = "Type", value = if (isIncome) "Income" else "Expense")
                if (transaction.note.isNotBlank()) {
                    DetailRow(label = "Notes", value = transaction.note)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
