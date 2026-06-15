package com.example.coustomerapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Login : Screen("login", "Login")
    object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    object InstallationTracker : Screen("installation_tracker", "Tracker", Icons.Default.Timeline)
    object InstallationJourney : Screen("installation_journey", "Journey")
    object MaterialDispatches : Screen("material_dispatches", "Dispatches", Icons.Default.Inventory)
    object ServiceRequests : Screen("service_requests", "Service", Icons.Default.Handyman)
    object Payments : Screen("payments", "Payments", Icons.Default.Payments)
    object Analytics : Screen("analytics", "Analytics", Icons.Default.Leaderboard)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.InstallationTracker,
    Screen.ServiceRequests,
    Screen.Payments,
    Screen.Settings
)
