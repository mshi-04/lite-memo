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

// GitHub Pages (docs/privacy/) で公開しているプライバシーポリシー
private const val PRIVACY_POLICY_URL = "https://mshi-04.github.io/lite-memo/privacy/"

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
    val filePickerNotFoundMessage = stringResource(R.string.settings_file_picker_not_found)

    val launchFilePicker: (() -> Unit) -> Unit = { launch ->
        try {
            launch()
        } catch (_: ActivityNotFoundException) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = filePickerNotFoundMessage,
                    withDismissAction = true
                )
            }
        }
    }

    val snackbarMessages = mapOf(
        SettingsSnackbarEvent.ExportSuccess to exportSuccessMessage,
        SettingsSnackbarEvent.ExportError to exportErrorMessage,
        SettingsSnackbarEvent.ImportSuccess to importSuccessMessage,
        SettingsSnackbarEvent.ImportError to importErrorMessage,
        SettingsSnackbarEvent.AppLockAuthenticationFailed to appLockAuthenticationFailedMessage,
        SettingsSnackbarEvent.AppLockAuthenticationCanceled to appLockAuthenticationCanceledMessage,
        SettingsSnackbarEvent.AppLockNoDeviceCredential to appLockNoDeviceCredentialMessage,
        SettingsSnackbarEvent.AppLockUnavailable to appLockUnavailableMessage
    )

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvent.collect { event ->
            snackbarHostState.showSnackbar(
                message = snackbarMessages.getValue(event),
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
                if (viewModel.beginAppLockAuthentication()) {
                    onRequestAppLockAuthentication { result ->
                        viewModel.onAppLockEnableAuthenticationResult(result)
                    }
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
        onExportClick = { launchFilePicker { exportLauncher.launch(defaultExportFileName()) } },
        onImportClick = { launchFilePicker { importLauncher.launch(arrayOf("application/json")) } },
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
