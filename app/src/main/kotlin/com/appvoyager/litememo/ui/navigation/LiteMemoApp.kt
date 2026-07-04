package com.appvoyager.litememo.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.appvoyager.litememo.R
import com.appvoyager.litememo.ui.auth.AppLockAuthenticationResult
import com.appvoyager.litememo.ui.component.BannerAd
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
fun LiteMemoApp(
    onRequestAppLockAuthentication: ((AppLockAuthenticationResult) -> Unit) -> Unit,
    viewModel: LiteMemoAppViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    var memoEditPopInFlight by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val memoDeletedMessage = stringResource(R.string.memo_deleted_message)
    val undoLabel = stringResource(R.string.undo_label)
    val restoreMemoErrorMessage = stringResource(R.string.memo_restore_failed_message)
    val draftErrorMessage = stringResource(R.string.memo_edit_draft_error_message)
    val saveMemoErrorMessage = stringResource(R.string.memo_save_error_message)
    val deleteMemoErrorMessage = stringResource(R.string.memo_delete_error_message)
    val shareErrorMessage = stringResource(R.string.share_memo_error)
    val browserNotFoundMessage = stringResource(R.string.settings_browser_not_found)
    val showErrorSnackbar: (String) -> Unit = remember(coroutineScope, snackbarHostState) {
        { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    withDismissAction = true
                )
            }
        }
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

    LaunchedEffect(currentDestination?.route) {
        memoEditPopInFlight = false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                Column {
                    BannerAd()
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = LiteMemoDestination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(
                route = LiteMemoDestination.Home.route,
                enterTransition = { bottomTabEnterTransition(this) },
                exitTransition = { bottomTabExitTransition(this) },
                popEnterTransition = { bottomTabEnterTransition(this) },
                popExitTransition = { bottomTabExitTransition(this) }
            ) {
                HomeRoute(
                    onMemoClick = { memoId ->
                        navController.navigateOnce(memoEditRouteWithId(memoId))
                    },
                    onCreateMemoClick = {
                        navController.navigateOnce(MEMO_EDIT_BASE)
                    },
                    onShareError = {
                        showErrorSnackbar(shareErrorMessage)
                    },
                    snackbarHostState = snackbarHostState
                )
            }
            composable(
                route = LiteMemoDestination.Calendar.route,
                enterTransition = { bottomTabEnterTransition(this) },
                exitTransition = { bottomTabExitTransition(this) },
                popEnterTransition = { bottomTabEnterTransition(this) },
                popExitTransition = { bottomTabExitTransition(this) }
            ) {
                CalendarRoute(
                    onMemoClick = { memoId ->
                        navController.navigateOnce(memoEditRouteWithId(memoId))
                    },
                    onCreateMemoClick = { createdAt ->
                        navController.navigateOnce(memoEditRouteWithCreatedAt(createdAt))
                    }
                )
            }
            composable(
                route = LiteMemoDestination.Settings.route,
                enterTransition = { bottomTabEnterTransition(this) },
                exitTransition = { bottomTabExitTransition(this) },
                popEnterTransition = { bottomTabEnterTransition(this) },
                popExitTransition = { bottomTabExitTransition(this) }
            ) {
                SettingsRoute(
                    snackbarHostState = snackbarHostState,
                    onRequestAppLockAuthentication = onRequestAppLockAuthentication,
                    onOpenSourceLicenseClick = {
                        navController.navigateOnce(OSS_LICENSES_ROUTE)
                    },
                    onTagManageClick = {
                        navController.navigateOnce(TAG_MANAGE_ROUTE)
                    },
                    onTrashClick = {
                        navController.navigateOnce(TRASH_ROUTE)
                    }
                )
            }
            composable(TRASH_ROUTE) {
                TrashRoute(
                    onNavigateBack = { navController.popBackStackIfResumed() },
                    snackbarHostState = snackbarHostState
                )
            }
            composable(TAG_MANAGE_ROUTE) {
                TagManageRoute(
                    onNavigateBack = { navController.popBackStackIfResumed() },
                    snackbarHostState = snackbarHostState
                )
            }
            composable(OSS_LICENSES_ROUTE) {
                OssLicensesRoute(
                    onNavigateBack = { navController.popBackStackIfResumed() },
                    onOpenUrlError = {
                        showErrorSnackbar(browserNotFoundMessage)
                    }
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
                    onNavigateBack = {
                        navController.popBackStackIfResumed(
                            isPopInFlight = { memoEditPopInFlight },
                            setPopInFlight = { memoEditPopInFlight = it },
                            deferUntilResumed = true
                        )
                    },
                    onShareError = {
                        showErrorSnackbar(shareErrorMessage)
                    },
                    onDraftError = {
                        showErrorSnackbar(draftErrorMessage)
                    },
                    onSaveError = {
                        showErrorSnackbar(saveMemoErrorMessage)
                    },
                    onDeleteError = {
                        showErrorSnackbar(deleteMemoErrorMessage)
                    },
                    onMemoDeleted = { memoId ->
                        navController.popBackStackIfResumed(
                            isPopInFlight = { memoEditPopInFlight },
                            setPopInFlight = { memoEditPopInFlight = it },
                            deferUntilResumed = true
                        )
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

// 連打対策: 遷移アニメーション中の多重 pop / navigate を一元的に抑止する。
// 現在の画面が RESUMED（遷移完了・最前面）のときだけ操作を実行する。
private fun NavController.isCurrentEntryResumed(): Boolean =
    currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true

private fun NavController.popBackStackIfResumed(
    isPopInFlight: () -> Boolean = { false },
    setPopInFlight: (Boolean) -> Unit = {},
    deferUntilResumed: Boolean = false
) {
    val entry = currentBackStackEntry
    if (isPopInFlight() || entry == null) return
    if (isCurrentEntryResumed()) {
        setPopInFlight(true)
        if (!popBackStack()) setPopInFlight(false)
    } else if (deferUntilResumed) {
        setPopInFlight(true)
        lateinit var observer: LifecycleEventObserver
        observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    entry.lifecycle.removeObserver(observer)
                    if (!popBackStack()) setPopInFlight(false)
                }

                Lifecycle.Event.ON_DESTROY -> {
                    entry.lifecycle.removeObserver(observer)
                    setPopInFlight(false)
                }

                else -> Unit
            }
        }
        entry.lifecycle.addObserver(observer)
    }
}

private fun NavController.navigateOnce(route: String) {
    if (isCurrentEntryResumed()) {
        navigate(route) { launchSingleTop = true }
    }
}

private fun bottomTabEnterTransition(
    scope: AnimatedContentTransitionScope<NavBackStackEntry>
): EnterTransition {
    val direction = bottomTabTransitionDirection(scope) ?: return EnterTransition.None
    return slideInHorizontally(
        animationSpec = tween(BOTTOM_TAB_TRANSITION_DURATION_MS),
        initialOffsetX = { width -> width / 4 * direction }
    ) + fadeIn(animationSpec = tween(BOTTOM_TAB_FADE_IN_DURATION_MS))
}

private fun bottomTabExitTransition(
    scope: AnimatedContentTransitionScope<NavBackStackEntry>
): ExitTransition {
    val direction = bottomTabTransitionDirection(scope) ?: return ExitTransition.None
    return slideOutHorizontally(
        animationSpec = tween(BOTTOM_TAB_TRANSITION_DURATION_MS),
        targetOffsetX = { width -> -width / 4 * direction }
    ) + fadeOut(animationSpec = tween(BOTTOM_TAB_FADE_OUT_DURATION_MS))
}

private fun bottomTabTransitionDirection(
    scope: AnimatedContentTransitionScope<NavBackStackEntry>
): Int? {
    val initialIndex = bottomTabRouteIndex(scope.initialState.destination.route) ?: return null
    val targetIndex = bottomTabRouteIndex(scope.targetState.destination.route) ?: return null
    if (initialIndex == targetIndex) return null
    return if (targetIndex > initialIndex) 1 else -1
}

private fun bottomTabRouteIndex(route: String?): Int? {
    val index = LiteMemoDestination.entries.indexOfFirst { destination ->
        destination.route == route
    }
    return index.takeIf { it >= 0 }
}

private const val BOTTOM_TAB_TRANSITION_DURATION_MS = 220
private const val BOTTOM_TAB_FADE_IN_DURATION_MS = 160
private const val BOTTOM_TAB_FADE_OUT_DURATION_MS = 120
