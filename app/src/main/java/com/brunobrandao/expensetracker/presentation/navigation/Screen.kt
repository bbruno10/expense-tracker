package com.brunobrandao.expensetracker.presentation.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object AddTransaction : Screen("add_transaction")
    data object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: Long) = "edit_transaction/$transactionId"
    }
    data object History : Screen("history")
    data object Chart : Screen("chart")
    data object Settings : Screen("settings")
    data object Login : Screen("login")
    data object Signup : Screen("signup")
    data object Recurring : Screen("recurring")
    data object EditRecurring : Screen("edit_recurring/{recurringId}") {
        fun createRoute(id: Long) = "edit_recurring/$id"
    }
    data object ManageCategories : Screen("manage_categories")
}
