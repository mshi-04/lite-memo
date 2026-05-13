package com.appvoyager.litememo.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.appvoyager.litememo.ui.screen.CalendarPlaceholderScreen
import com.appvoyager.litememo.ui.screen.HomeRoute
import com.appvoyager.litememo.ui.screen.SettingsPlaceholderScreen

@Composable
fun LiteMemoApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route ?: LiteMemoDestination.Home.route

    Scaffold(
        floatingActionButton = {
            if (currentRoute == LiteMemoDestination.Home.route) {
                FloatingActionButton(onClick = {}) {
                    Text(text = "+")
                }
            }
        },
        bottomBar = {
            NavigationBar {
                LiteMemoDestination.entries.forEach { destination ->
                    val selected = currentDestination
                        ?.hierarchy
                        ?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.labelResId)
                            )
                        },
                        label = { Text(text = stringResource(destination.labelResId)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = LiteMemoDestination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(LiteMemoDestination.Home.route) {
                HomeRoute()
            }
            composable(LiteMemoDestination.Calendar.route) {
                CalendarPlaceholderScreen()
            }
            composable(LiteMemoDestination.Settings.route) {
                SettingsPlaceholderScreen()
            }
        }
    }
}
