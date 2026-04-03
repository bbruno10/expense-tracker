package com.example.expensetracker.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTest {

    @Test
    fun `Home has correct route`() {
        assertEquals("home", Screen.Home.route)
    }

    @Test
    fun `AddTransaction has correct route`() {
        assertEquals("add_transaction", Screen.AddTransaction.route)
    }

    @Test
    fun `EditTransaction has parameterized route`() {
        assertTrue(Screen.EditTransaction.route.contains("{transactionId}"))
    }

    @Test
    fun `EditTransaction createRoute builds correct path`() {
        val route = Screen.EditTransaction.createRoute(42L)
        assertEquals("edit_transaction/42", route)
    }

    @Test
    fun `EditTransaction createRoute with zero id`() {
        val route = Screen.EditTransaction.createRoute(0L)
        assertEquals("edit_transaction/0", route)
    }

    @Test
    fun `History has correct route`() {
        assertEquals("history", Screen.History.route)
    }

    @Test
    fun `Chart has correct route`() {
        assertEquals("chart", Screen.Chart.route)
    }

    @Test
    fun `Settings has correct route`() {
        assertEquals("settings", Screen.Settings.route)
    }

    @Test
    fun `all routes are unique`() {
        val routes = listOf(
            Screen.Home.route,
            Screen.AddTransaction.route,
            Screen.EditTransaction.route,
            Screen.History.route,
            Screen.Chart.route,
            Screen.Settings.route
        )
        assertEquals(routes.size, routes.toSet().size)
    }
}
