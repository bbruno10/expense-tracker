package com.brunobrandao.expensetracker.presentation.navigation

import com.brunobrandao.expensetracker.presentation.categories.ManageCategoriesScreen
import com.brunobrandao.expensetracker.presentation.recurring.EditRecurringScreen
import com.brunobrandao.expensetracker.presentation.recurring.RecurringScreen
import com.brunobrandao.expensetracker.presentation.settings.SettingsScreen
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.brunobrandao.expensetracker.presentation.add.AddTransactionScreen
import com.brunobrandao.expensetracker.presentation.auth.AuthViewModel
import com.brunobrandao.expensetracker.presentation.auth.LoginScreen
import com.brunobrandao.expensetracker.presentation.auth.SignupScreen
import com.brunobrandao.expensetracker.presentation.chart.ChartScreen
import com.brunobrandao.expensetracker.presentation.history.HistoryScreen
import com.brunobrandao.expensetracker.presentation.home.HomeScreen
import com.brunobrandao.expensetracker.presentation.home.HomeViewModel

private val AlertRedBackground = Color(0xFFFFF0F0)

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.History, "History", Icons.Filled.History, Icons.Outlined.History),
    BottomNavItem(Screen.AddTransaction, "Add", Icons.Filled.Add, Icons.Filled.Add),
    BottomNavItem(Screen.Chart, "Charts", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    BottomNavItem(Screen.Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
fun ExpenseNavHost(navController: NavHostController) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val startDestination = if (authViewModel.isLoggedIn) Screen.Home.route else Screen.Login.route

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in listOf(
        Screen.Home.route,
        Screen.History.route,
        Screen.Chart.route,
        Screen.Settings.route
    )

    val homeViewModel: HomeViewModel = hiltViewModel()
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val isNegative = homeState.balance < 0 && !homeState.isLoading

    val backgroundColor by animateColorAsState(
        targetValue = if (isNegative && currentDestination?.route == Screen.Home.route) {
            AlertRedBackground
        } else {
            MaterialTheme.colorScheme.background
        },
        animationSpec = tween(durationMillis = 600),
        label = "bg_color"
    )

    Scaffold(
        containerColor = backgroundColor,
        bottomBar = {
            if (showBottomBar) {
                Box {
                    GooeyBottomNavBar(
                        items = bottomNavItems,
                        currentDestination = currentDestination,
                        onItemClick = { item ->
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = false
                                }
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    )

                    // Center FAB with "Add" label
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-20).dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FloatingActionButton(
                            onClick = {
                                navController.navigate(Screen.AddTransaction.route)
                            },
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 6.dp
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add transaction",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable(
                    route = Screen.Login.route,
                    enterTransition = { fadeIn(tween(200)) },
                    exitTransition = { fadeOut(tween(200)) }
                ) {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        },
                        onNavigateToSignup = {
                            navController.navigate(Screen.Signup.route)
                        }
                    )
                }

                composable(
                    route = Screen.Signup.route,
                    enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
                    exitTransition = { slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(300)) },
                    popEnterTransition = { slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(300)) },
                    popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
                ) {
                    SignupScreen(
                        onSignupSuccess = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        },
                        onNavigateToLogin = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = Screen.Home.route,
                    enterTransition = { fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.92f, animationSpec = tween(300)) },
                    exitTransition = { fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.96f, animationSpec = tween(200)) }
                ) {
                    HomeScreen(
                        onNavigateToHistory = {
                            navController.navigate(Screen.History.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = false
                                }
                                launchSingleTop = true
                                restoreState = false
                            }
                        },
                        onEditTransaction = { transactionId ->
                            navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                        },
                        viewModel = homeViewModel
                    )
                }

                composable(
                    route = Screen.AddTransaction.route,
                    enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
                    exitTransition = { slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(300)) },
                    popEnterTransition = { slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(300)) },
                    popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
                ) {
                    AddTransactionScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = Screen.EditTransaction.route,
                    arguments = listOf(navArgument("transactionId") { type = NavType.LongType }),
                    enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
                    exitTransition = { slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(300)) },
                    popEnterTransition = { slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(300)) },
                    popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
                ) { backStackEntry ->
                    val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
                    AddTransactionScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        transactionId = transactionId
                    )
                }

                composable(
                    route = Screen.History.route,
                    enterTransition = { fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.92f, animationSpec = tween(300)) },
                    exitTransition = { fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.96f, animationSpec = tween(200)) }
                ) {
                    HistoryScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onEditTransaction = { transactionId ->
                            navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                        }
                    )
                }

                composable(
                    route = Screen.Chart.route,
                    enterTransition = { fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.92f, animationSpec = tween(300)) },
                    exitTransition = { fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.96f, animationSpec = tween(200)) }
                ) {
                    ChartScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = Screen.Settings.route,
                    enterTransition = { fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.92f, animationSpec = tween(300)) },
                    exitTransition = { fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.96f, animationSpec = tween(200)) }
                ) {
                    SettingsScreen(
                        onSignOut = {
                            authViewModel.signOut()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onDeleteAccountSuccess = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onNavigateToRecurring = {
                            navController.navigate(Screen.Recurring.route)
                        },
                        onNavigateToManageCategories = {
                            navController.navigate(Screen.ManageCategories.route)
                        }
                    )
                }

                composable(
                    route = Screen.ManageCategories.route,
                    enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
                    exitTransition = { slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(300)) },
                    popEnterTransition = { slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(300)) },
                    popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
                ) {
                    ManageCategoriesScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.Recurring.route,
                    enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
                    exitTransition = { slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(300)) },
                    popEnterTransition = { slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(300)) },
                    popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
                ) {
                    RecurringScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToAdd = { navController.navigate(Screen.AddTransaction.route) },
                        onEditRule = { id -> navController.navigate(Screen.EditRecurring.createRoute(id)) }
                    )
                }

                composable(
                    route = Screen.EditRecurring.route,
                    arguments = listOf(navArgument("recurringId") { type = NavType.LongType }),
                    enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
                    exitTransition = { slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(300)) },
                    popEnterTransition = { slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(300)) },
                    popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
                ) { backStackEntry ->
                    val recurringId = backStackEntry.arguments?.getLong("recurringId") ?: 0L
                    EditRecurringScreen(
                        recurringId = recurringId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
