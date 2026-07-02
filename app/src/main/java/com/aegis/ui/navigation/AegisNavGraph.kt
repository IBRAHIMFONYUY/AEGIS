package com.aegis.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aegis.agents.GuardianCore
import com.aegis.data.repository.*
import com.aegis.ui.academy.AcademyScreen
import com.aegis.ui.assistant.AssistantScreen
import com.aegis.ui.dashboard.DashboardScreen
import com.aegis.ui.settings.SettingsScreen
import com.aegis.ui.threatlog.ThreatLogScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Dashboard)
    data object ThreatLog : Screen("threat_log", "Threat Log", Icons.Filled.Warning)
    data object Assistant : Screen("assistant", "AI Assistant", Icons.Filled.SmartToy)
    data object Academy : Screen("academy", "Academy", Icons.Filled.School)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.ThreatLog,
    Screen.Assistant,
    Screen.Academy,
    Screen.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AegisNavGraph(
    guardianCore: GuardianCore,
    threatRepository: ThreatRepository,
    safetyRepository: SafetyRepository,
    learningRepository: LearningRepository,
    settingsRepository: SettingsRepository
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    guardianCore = guardianCore,
                    safetyRepository = safetyRepository,
                    threatRepository = threatRepository,
                    navController = navController
                )
            }
            composable(Screen.ThreatLog.route) {
                ThreatLogScreen(
                    threatRepository = threatRepository
                )
            }
            composable(Screen.Assistant.route) {
                AssistantScreen(guardianCore = guardianCore)
            }
            composable(Screen.Academy.route) {
                AcademyScreen(
                    learningRepository = learningRepository,
                    guardianCore = guardianCore
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    settingsRepository = settingsRepository,
                    guardianCore = guardianCore
                )
            }
        }
    }
}
