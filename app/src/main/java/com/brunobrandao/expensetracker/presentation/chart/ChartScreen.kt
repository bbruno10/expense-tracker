package com.brunobrandao.expensetracker.presentation.chart

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brunobrandao.expensetracker.R
import com.brunobrandao.expensetracker.data.preferences.Currency
import com.brunobrandao.expensetracker.domain.model.Category
import com.brunobrandao.expensetracker.domain.model.DefaultCategories
import com.brunobrandao.expensetracker.presentation.home.PeriodNavigator
import com.brunobrandao.expensetracker.presentation.home.TimePeriod
import com.brunobrandao.expensetracker.presentation.util.CurrencyFormatter
import com.brunobrandao.expensetracker.ui.theme.LocalFinanceColors
import java.util.Locale

@Composable
fun ChartScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChartViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refresh()
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
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Title
            item {
                Text(
                    text = stringResource(R.string.chart_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Period Filter
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TimePeriod.entries.forEach { period ->
                        FilterChip(
                            selected = state.selectedPeriod == period,
                            onClick = { viewModel.onPeriodChanged(period) },
                            label = { Text(period.label, style = MaterialTheme.typography.labelSmall) },
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
                    canGoNext = state.periodOffset < 0,
                    onPrevious = { viewModel.onPreviousPeriod() },
                    onNext = { viewModel.onNextPeriod() }
                )
            }

            if (state.expensesByCategory.isEmpty()) {
                // Empty state message
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.chart_empty_state),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Donut Chart Card with Legend
                item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.chart_expenses_by_category),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DonutWithLegend(
                            expensesByCategory = state.expensesByCategory,
                            categoriesMap = state.categoriesMap,
                            totalExpenses = state.totalExpenses,
                            currency = state.currency
                        )
                    }
                }
            }

            // Bar Chart Breakdown Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.chart_category_breakdown),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val maxAmount = state.expensesByCategory.values.maxOrNull() ?: 1.0

                        state.expensesByCategory.forEach { (key, amount) ->
                            BarChartRow(
                                category = state.categoriesMap[key] ?: DefaultCategories.fallback(key),
                                amount = amount,
                                maxAmount = maxAmount,
                                currency = state.currency
                            )
                            Spacer(modifier = Modifier.height(7.dp))
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.chart_total_expenses_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.format(state.totalExpenses, state.currency),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = LocalFinanceColors.current.expense
                            )
                        }
                    }
                }
            }

            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun DonutWithLegend(
    expensesByCategory: Map<String, Double>,
    categoriesMap: Map<String, Category>,
    totalExpenses: Double,
    currency: Currency
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animationProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "donut"
    )
    LaunchedEffect(Unit) { animationPlayed = true }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Donut
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(120.dp)) {
                val strokeWidth = 28f
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(
                    (size.width - diameter) / 2f,
                    (size.height - diameter) / 2f
                )
                var startAngle = -90f

                expensesByCategory.forEach { (key, amount) ->
                    val category = categoriesMap[key] ?: DefaultCategories.fallback(key)
                    val sweepAngle = (amount / totalExpenses * 360f).toFloat() * animationProgress
                    drawArc(
                        color = category.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += sweepAngle
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = CurrencyFormatter.formatCompact(totalExpenses, currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.chart_total_spent),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Legend
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            expensesByCategory.forEach { (key, amount) ->
                val category = categoriesMap[key] ?: DefaultCategories.fallback(key)
                val percentage = if (totalExpenses > 0) (amount / totalExpenses * 100).toInt() else 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(category.color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BarChartRow(
    category: Category,
    amount: Double,
    maxAmount: Double,
    currency: Currency
) {
    val progress = (amount / maxAmount).toFloat().coerceIn(0f, 1f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (category.name.length > 6) category.name.take(5) + "." else category.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(50.dp),
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(26.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(26.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(category.color),
                contentAlignment = Alignment.CenterStart
            ) {
                if (progress > 0.2f) {
                    Text(
                        text = CurrencyFormatter.formatCompact(amount, currency),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp),
                        maxLines = 1
                    )
                }
            }
            // Show value outside bar if bar is too small
            if (progress <= 0.2f) {
                Text(
                    text = CurrencyFormatter.formatCompact(amount, currency),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = category.color,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = (progress * 200 + 8).dp.coerceAtLeast(8.dp)),
                    maxLines = 1
                )
            }
        }
    }
}
