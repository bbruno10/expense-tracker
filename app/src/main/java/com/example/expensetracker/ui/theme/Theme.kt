package com.example.expensetracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = NeutralWhite,
    primaryContainer = GreenPrimaryContainer,
    onPrimaryContainer = OnGreenPrimaryContainer,
    secondary = Teal,
    onSecondary = NeutralWhite,
    secondaryContainer = TealContainer,
    onSecondaryContainer = OnTealContainer,
    tertiary = Amber,
    onTertiary = NeutralWhite,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    background = SurfaceLight,
    onBackground = NeutralDark,
    surface = SurfaceLight,
    onSurface = NeutralDark,
    surfaceVariant = SurfaceContainerLight,
    onSurfaceVariant = NeutralMedium,
    error = ExpenseRed,
    onError = NeutralWhite,
    outline = Color(0xFF73796E),
    outlineVariant = Color(0xFFC3C8BB)
)

private val DarkColorScheme = darkColorScheme(
    primary = GreenPrimaryLight,
    onPrimary = OnGreenPrimaryContainer,
    primaryContainer = GreenPrimaryDark,
    onPrimaryContainer = GreenPrimaryContainer,
    secondary = Color(0xFF80D5CB),
    onSecondary = OnTealContainer,
    secondaryContainer = TealDark,
    onSecondaryContainer = TealContainer,
    tertiary = Color(0xFFFFD466),
    onTertiary = OnAmberContainer,
    tertiaryContainer = Color(0xFF5C4300),
    onTertiaryContainer = AmberContainer,
    background = SurfaceDark,
    onBackground = Color(0xFFE2E3DE),
    surface = SurfaceDark,
    onSurface = Color(0xFFE2E3DE),
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = Color(0xFFC3C8BB),
    error = Color(0xFFFF897D),
    onError = Color(0xFF601410),
    outline = Color(0xFF8D9286),
    outlineVariant = Color(0xFF43483E)
)

@Composable
fun ExpenseTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
