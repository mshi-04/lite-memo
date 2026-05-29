package com.appvoyager.litememo.ui.navigation

import android.app.Activity
import android.app.KeyguardManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.appvoyager.litememo.R
import com.appvoyager.litememo.ui.auth.AppLockAuthenticationResult
import com.appvoyager.litememo.ui.screen.CalendarRoute
import com.appvoyager.litememo.ui.screen.HomeRoute
import com.appvoyager.litememo.ui.screen.MemoEditRoute
import com.appvoyager.litememo.ui.screen.OssLicensesRoute
import com.appvoyager.litememo.ui.screen.SettingsRoute
import com.appvoyager.litememo.ui.screen.TagManageRoute
import com.appvoyager.litememo.ui.screen.TrashRoute
import com.appvoyager.litememo.ui.viewmodel.LiteMemoAppViewModel
import kotlinx.coroutines.launch

private const val OSS_LICENSES_ROUTE = "oss_licenses"
private const val TAG_MANAGE_ROUTE = "tag_manage"
private const val TRASH_ROUTE = "trash"
private const val MEMO_EDIT_BASE = "memo_edit"
private const val MEMO_EDIT_ROUTE = "$MEMO_EDIT_BASE?memoId={memoId}&createdAt={createdAt}"
private fun memoEditRouteWithId(memoId: String) = "$MEMO_EDIT_BASE?memoId=${Uri.encode(memoId)}"
private fun memoEditRouteWithCreatedAt(createdAt: Long) = "$MEMO_EDIT_BASE?createdAt=$createdAt"

@Composable
fun LiteMemoApp(viewModel: LiteMemoAppViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val memoDeletedMessage = stringResource(R.string.memo_deleted_message)
    val undoLabel = stringResource(R.string.undo_label)
    val restoreMemoErrorMessage = stringResource(R.string.memo_restore_failed_message)
    val draftErrorMessage = stringResource(R.string.memo_edit_draft_error_message)
    val shareErrorMessage = stringResource(R.string.share_memo_error)
    val appLockPromptTitle = stringResource(R.string.app_lock_prompt_title)
    val appLockPromptSubtitle = stringResource(R.string.app_lock_prompt_subtitle)

    var appLockAuthenticationCallback by remember {
        mutableStateOf<((AppLockAuthenticationResult) -> Unit)?>(null)
    }
    val appLockAuthenticationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = appLockAuthenticationCallback ?: return@rememberLauncherForActivityResult
        appLockAuthenticationCallback = null
        val authenticationResult = if (result.resultCode == Activity.RESULT_OK) {
            AppLockAuthenticationResult.SUCCEEDED
        } else {
            AppLockAuthenticationResult.CANCELED
        }
        callback(authenticationResult)
    }

    val showBottomBar = LiteMemoDestination.entries.any { dest ->
        currentDestination?.hierarchy?.any { it.route == dest.route } == true
    }

    LaunchedEffect(viewModel, snackbarHostState, restoreMemoErrorMessage) {
        viewModel.restoreMemoErrorEvent.collect {
            snackbarHostState.showSnackbar(
                message = restoreMemoErrorMessage,
                withDismissAction = true
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                    },
                    onShareError = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = shareErrorMessage,
                                withDismissAction = true
                            )
                        }
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
                    snackbarHostState = snackbarHostState,
                    onRequestAppLockAuthentication = { callback ->
                        if (appLockAuthenticationCallback != null) {
                            callback(AppLockAuthenticationResult.CANCELED)
                        } else {
                            val keyguardManager = context.getSystemService(
                                KeyguardManager::class.java
                            )
                            if (keyguardManager == null) {
                                callback(AppLockAuthenticationResult.UNAVAILABLE)
                            } else if (!keyguardManager.isDeviceSecure) {
                                callback(AppLockAuthenticationResult.NO_DEVICE_CREDENTIAL)
                            } else {
                                @Suppress("DEPRECATION")
                                val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                                    appLockPromptTitle,
                                    appLockPromptSubtitle
                                )
                                if (intent == null) {
                                    callback(AppLockAuthenticationResult.UNAVAILABLE)
                                } else {
                                    appLockAuthenticationCallback = callback
                                    appLockAuthenticationLauncher.launch(intent)
                                }
                            }
                        }
                    },
                    onOpenSourceLicenseClick = {
                        navController.navigate(OSS_LICENSES_ROUTE)
                    },
                    onTagManageClick = {
                        navController.navigate(TAG_MANAGE_ROUTE)
                    },
                    onTrashClick = {
                        navController.navigate(TRASH_ROUTE)
                    }
                )
            }
            composable(TRASH_ROUTE) {
                TrashRoute(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(TAG_MANAGE_ROUTE) {
                TagManageRoute(
                    onNavigateBack = { navController.popBackStack() }
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
                    onNavigateBack = { navController.popBackStack() },
                    onShareError = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = shareErrorMessage,
                                withDismissAction = true
                            )
                        }
                    },
                    onDraftError = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = draftErrorMessage,
                                withDismissAction = true
                            )
                        }
                    },
                    onMemoDeleted = { memoId ->
                        navController.popBackStack()
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = memoDeletedMessage,
                                actionLabel = undoLabel,
                                withDismissAction = true,
                                duration = SnackbarDuration.Long
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.restoreMemo(memoId)
                            }
                        }
                    }
                )
            }
        }
    }
}
