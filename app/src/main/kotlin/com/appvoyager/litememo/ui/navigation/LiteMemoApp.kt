package com.appvoyager.litememo.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.appvoyager.litememo.ui.screen.CalendarRoute
import com.appvoyager.litememo.ui.screen.HomeRoute
import com.appvoyager.litememo.ui.screen.MemoEditRoute
import com.appvoyager.litememo.ui.screen.OssLicensesRoute
import com.appvoyager.litememo.ui.screen.SettingsRoute

private const val OSS_LICENSES_ROUTE = "oss_licenses"
private const val MEMO_EDIT_BASE = "memo_edit"
private const val MEMO_EDIT_ROUTE = "$MEMO_EDIT_BASE?memoId={memoId}&createdAt={createdAt}"
private fun memoEditRouteWithId(memoId: String) = "$MEMO_EDIT_BASE?memoId=${Uri.encode(memoId)}"
private fun memoEditRouteWithCreatedAt(createdAt: Long) =
    "$MEMO_EDIT_BASE?createdAt=$createdAt"

@Composable
fun LiteMemoApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val showBottomBar = LiteMemoDestination.entries.any { dest ->
        currentDestination?.hierarchy?.any { it.route == dest.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = LiteMemoDestination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(LiteMemoDestination.Home.route) {
                HomeRoute(
                    onMemoClick = { memoId ->
                        navController.navigate(memoEditRouteWithId(memoId))
                    },
                    onCreateMemoClick = {
                        navController.navigate(MEMO_EDIT_BASE)
                    }
                )
            }
            composable(LiteMemoDestination.Calendar.route) {
                CalendarRoute(
                    onMemoClick = { memoId ->
                        navController.navigate(memoEditRouteWithId(memoId))
                    },
                    onCreateMemoClick = { createdAt ->
                        navController.navigate(memoEditRouteWithCreatedAt(createdAt))
                    }
                )
            }
            composable(LiteMemoDestination.Settings.route) {
                SettingsRoute(
                    onOpenSourceLicenseClick = {
                        navController.navigate(OSS_LICENSES_ROUTE)
                    }
                )
            }
            composable(OSS_LICENSES_ROUTE) {
                OssLicensesRoute(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = MEMO_EDIT_ROUTE,
                arguments = listOf(
                    navArgument("memoId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("createdAt") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) {
                MemoEditRoute(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
