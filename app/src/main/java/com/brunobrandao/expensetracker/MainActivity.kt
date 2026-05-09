package com.brunobrandao.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.brunobrandao.expensetracker.data.preferences.ThemeMode
import com.brunobrandao.expensetracker.data.preferences.UserPreferences
import com.brunobrandao.expensetracker.data.preferences.UserPreferencesRepository
import com.brunobrandao.expensetracker.presentation.navigation.ExpenseNavHost
import com.brunobrandao.expensetracker.presentation.splash.SplashScreen
import com.brunobrandao.expensetracker.presentation.util.CurrencyFormatter
import com.brunobrandao.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var showSplash by remember { mutableStateOf(true) }

            val preferences by preferencesRepository.userPreferences.collectAsState(
                initial = UserPreferences()
            )

            LaunchedEffect(preferences.currency) {
                CurrencyFormatter.setCurrency(preferences.currency)
            }

            val darkTheme = when (preferences.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> null
            }

            if (showSplash) {
                SplashScreen(onSplashFinished = { showSplash = false })
            } else {
                ExpenseTrackerTheme(darkTheme = darkTheme) {
                    val navController = rememberNavController()
                    ExpenseNavHost(navController = navController)
                }
            }
        }
    }
}
