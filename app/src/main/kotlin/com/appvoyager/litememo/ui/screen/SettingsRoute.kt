package com.appvoyager.litememo.ui.screen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appvoyager.litememo.R
import com.appvoyager.litememo.domain.model.value.ExportFileReference
import com.appvoyager.litememo.ui.auth.AppLockAuthenticationResult
import com.appvoyager.litememo.ui.viewmodel.SettingsSnackbarEvent
import com.appvoyager.litememo.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// TODO: リリース前に正式なプライバシーポリシー URL に必ず差し替える
private const val PRIVACY_POLICY_URL = "https://example.com/privacy-policy"

private fun defaultExportFileName(): String {
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    return "lite-memo-export-${LocalDateTime.now().format(formatter)}.json"
}

@Composable
fun SettingsRoute(
    snackbarHostState: SnackbarHostState,
    onRequestAppLockAuthentication: ((AppLockAuthenticationResult) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    onOpenSourceLicenseClick: () -> Unit = {},
    onTagManageClick: () -> Unit = {},
    onTrashClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportMemos(it.toExportFileReference()) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.onImportFileSelected(it.toExportFileReference()) }
    }

    val exportSuccessMessage = stringResource(R.string.settings_export_success)
    val exportErrorMessage = stringResource(R.string.settings_export_error)
    val importSuccessMessage = stringResource(R.string.settings_import_success)
    val importErrorMessage = stringResource(R.string.settings_import_error)
    val appLockAuthenticationFailedMessage = stringResource(R.string.app_lock_auth_failed)
    val appLockAuthenticationCanceledMessage =
        stringResource(R.string.settings_app_lock_auth_canceled)
    val appLockNoDeviceCredentialMessage =
        stringResource(R.string.settings_app_lock_no_device_credential)
    val appLockUnavailableMessage = stringResource(R.string.settings_app_lock_unavailable)
    val browserNotFoundMessage = stringResource(R.string.settings_browser_not_found)

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvent.collect { event ->
            val message = when (event) {
                SettingsSnackbarEvent.ExportSuccess -> exportSuccessMessage

                SettingsSnackbarEvent.ExportError -> exportErrorMessage

                SettingsSnackbarEvent.ImportSuccess -> importSuccessMessage

                SettingsSnackbarEvent.ImportError -> importErrorMessage

                SettingsSnackbarEvent.AppLockAuthenticationFailed ->
                    appLockAuthenticationFailedMessage

                SettingsSnackbarEvent.AppLockAuthenticationCanceled ->
                    appLockAuthenticationCanceledMessage

                SettingsSnackbarEvent.AppLockNoDeviceCredential ->
                    appLockNoDeviceCredentialMessage

                SettingsSnackbarEvent.AppLockUnavailable -> appLockUnavailableMessage
            }
            snackbarHostState.showSnackbar(
                message = message,
                withDismissAction = true
            )
        }
    }

    SettingsScreen(
        uiState = uiState,
        onThemeModeSelected = { viewModel.setThemeMode(it) },
        onMemoSortOrderSelected = { viewModel.setMemoSortOrder(it) },
        onAppLockEnabledChange = { enabled ->
            if (enabled) {
                onRequestAppLockAuthentication { result ->
                    viewModel.onAppLockEnableAuthenticationResult(result)
                }
            } else {
                viewModel.setAppLockEnabled(false)
            }
        },
        onExpandThemeDropdown = { viewModel.expandThemeDropdown() },
        onCollapseThemeDropdown = { viewModel.collapseThemeDropdown() },
        onExpandSortOrder = { viewModel.expandSortOrder() },
        onCollapseSortOrder = { viewModel.collapseSortOrder() },
        onTagManageClick = onTagManageClick,
        onTrashClick = onTrashClick,
        onExportClick = { exportLauncher.launch(defaultExportFileName()) },
        onImportClick = { importLauncher.launch(arrayOf("application/json")) },
        onConfirmImport = { viewModel.confirmImport() },
        onDismissImportConfirmDialog = { viewModel.dismissImportConfirmDialog() },
        onPrivacyPolicyClick = {
            try {
                val intent = Intent(Intent.ACTION_VIEW, PRIVACY_POLICY_URL.toUri())
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = browserNotFoundMessage,
                        withDismissAction = true
                    )
                }
            }
        },
        onOpenSourceLicenseClick = onOpenSourceLicenseClick,
        modifier = modifier
    )
}

private fun Uri.toExportFileReference(): ExportFileReference = ExportFileReference(toString())
