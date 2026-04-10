package com.example.expensetracker.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetracker.data.preferences.Currency
import com.example.expensetracker.data.preferences.ThemeMode

private val GreenPrimary = Color(0xFF1D9E75)

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.showExportSuccess) {
        if (state.showExportSuccess) {
            snackbarHostState.showSnackbar("Data exported to Downloads folder!")
            viewModel.onEvent(SettingsEvent.DismissExportSuccess)
        }
    }

    // Currency Dialog
    if (state.showCurrencyDialog) {
        SelectionDialog(
            title = "Select Currency",
            options = Currency.entries.map { "${it.symbol}  ${it.displayName}" },
            selectedIndex = Currency.entries.indexOf(state.currency),
            onSelect = { index ->
                viewModel.onEvent(SettingsEvent.CurrencyChanged(Currency.entries[index]))
            },
            onDismiss = { viewModel.onEvent(SettingsEvent.ToggleCurrencyDialog) }
        )
    }

    // Theme Dialog
    if (state.showThemeDialog) {
        SelectionDialog(
            title = "Select Theme",
            options = ThemeMode.entries.map { it.displayName },
            selectedIndex = ThemeMode.entries.indexOf(state.themeMode),
            onSelect = { index ->
                viewModel.onEvent(SettingsEvent.ThemeModeChanged(ThemeMode.entries[index]))
            },
            onDismiss = { viewModel.onEvent(SettingsEvent.ToggleThemeDialog) }
        )
    }

    // About Dialog
    if (state.showAboutDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(SettingsEvent.ToggleAboutDialog) },
            title = {
                Text(
                    text = "About ExpenseTracker",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AboutRow(label = "Version", value = state.appVersion)
                    AboutRow(label = "Developer", value = "Bruno Brandão")
                    AboutRow(label = "Architecture", value = "Clean Architecture + MVVM")
                    AboutRow(label = "Tech Stack", value = "Kotlin, Jetpack Compose, Hilt, Room")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onEvent(SettingsEvent.ToggleAboutDialog) }
                ) {
                    Text("Close", color = GreenPrimary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Title
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // Preferences Section
        SectionHeader(title = "Preferences")

        SettingsCard {
            SettingsItem(
                icon = Icons.Default.AttachMoney,
                title = "Currency",
                subtitle = "${state.currency.symbol}  ${state.currency.displayName}",
                onClick = { viewModel.onEvent(SettingsEvent.ToggleCurrencyDialog) }
            )
            SettingsDivider()
            SettingsItem(
                icon = Icons.Default.DarkMode,
                title = "Theme",
                subtitle = state.themeMode.displayName,
                onClick = { viewModel.onEvent(SettingsEvent.ToggleThemeDialog) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Data Section
        SectionHeader(title = "Data")

        SettingsCard {
            SettingsItem(
                icon = Icons.Default.FileDownload,
                title = "Export to CSV",
                subtitle = "Save transactions to Downloads",
                onClick = { viewModel.onEvent(SettingsEvent.ExportData) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About Section
        SectionHeader(title = "About")

        SettingsCard {
            SettingsItem(
                icon = Icons.Default.Info,
                title = "About ExpenseTracker",
                subtitle = "Version ${state.appVersion}",
                onClick = { viewModel.onEvent(SettingsEvent.ToggleAboutDialog) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Snackbar
        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = GreenPrimary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GreenPrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 56.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun SelectionDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(index) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (index == selectedIndex) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (index == selectedIndex) GreenPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (index == selectedIndex) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = GreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (index < options.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
