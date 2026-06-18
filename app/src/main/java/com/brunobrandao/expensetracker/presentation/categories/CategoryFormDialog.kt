package com.brunobrandao.expensetracker.presentation.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val GreenPrimary = Color(0xFF1D9E75)

/**
 * Dumb composable: receives all state as parameters, exposes callbacks.
 * No business logic here — calling the use case is the ViewModel's responsibility.
 * Used by both ManageCategoriesScreen and AddTransactionScreen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryFormDialog(
    title: String,
    name: String,
    icon: String,
    colorIndex: Int,
    nameError: String?,
    onNameChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onColorChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val selectedColor = CATEGORY_COLORS.getOrElse(colorIndex) { CATEGORY_COLORS[0] }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Name ─────────────────────────────────────────────────────
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Hobbies") },
                    isError = nameError != null,
                    supportingText = nameError?.let { msg ->
                        { Text(msg, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        focusedLabelColor = GreenPrimary,
                        cursorColor = GreenPrimary
                    )
                )

                // ── Icon ──────────────────────────────────────────────────────
                Text(
                    "Icon",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CATEGORY_ICONS.forEach { emoji ->
                        val isSelected = emoji == icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) selectedColor.lightColor
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .then(
                                    if (isSelected) Modifier.border(
                                        1.5.dp, selectedColor.color, RoundedCornerShape(8.dp)
                                    ) else Modifier
                                )
                                .clickable { onIconChange(emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                // ── Colour ────────────────────────────────────────────────────
                Text(
                    "Colour",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CATEGORY_COLORS.forEachIndexed { index, option ->
                        val isSelected = index == colorIndex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(option.color)
                                .then(
                                    if (isSelected) Modifier
                                        .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                                        .border(4.5.dp, option.color, CircleShape)
                                    else Modifier
                                )
                                .clickable { onColorChange(index) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
