package com.example.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.example.expensetracker.data.preferences.ThemeMode
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.data.preferences.UserPreferencesRepository
import com.example.expensetracker.presentation.navigation.ExpenseNavHost
import com.example.expensetracker.presentation.util.CurrencyFormatter
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
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

            ExpenseTrackerTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                ExpenseNavHost(navController = navController)
            }
        }
    }
}
