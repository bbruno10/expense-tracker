package com.example.expensetracker.presentation.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object AddTransaction : Screen("add_transaction")
    data object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: Long) = "edit_transaction/$transactionId"
    }
    data object History : Screen("history")
    data object Chart : Screen("chart")
    data object Settings : Screen("settings")
}
