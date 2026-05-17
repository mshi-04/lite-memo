package com.appvoyager.litememo.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appvoyager.litememo.ui.viewmodel.SettingsViewModel

// TODO: リリース前に正式な URL に差し替える
private const val PRIVACY_POLICY_URL = "https://example.com/privacy-policy"

@Composable
fun SettingsRoute(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    SettingsScreen(
        uiState = uiState,
        onThemeModeSelected = { viewModel.setThemeMode(it) },
        onMemoSortOrderSelected = { viewModel.setMemoSortOrder(it) },
        onShowThemeDialog = { viewModel.showThemeDialog() },
        onDismissThemeDialog = { viewModel.dismissThemeDialog() },
        onExpandSortOrder = { viewModel.expandSortOrder() },
        onCollapseSortOrder = { viewModel.collapseSortOrder() },
        onPrivacyPolicyClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))
            context.startActivity(intent)
        },
        onOpenSourceLicenseClick = {
            // TODO: OSSライセンス画面を実装（oss-licenses-plugin の AGP 9.x 対応待ち）
        },
        modifier = modifier
    )
}
