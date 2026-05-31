package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.litememo.domain.model.MemoSortOrder
import com.appvoyager.litememo.domain.model.ThemeMode
import com.appvoyager.litememo.domain.model.value.ExportFileReference
import com.appvoyager.litememo.domain.repository.ExportFileRepository
import com.appvoyager.litememo.domain.usecase.ExportMemosUseCase
import com.appvoyager.litememo.domain.usecase.ImportMemosUseCase
import com.appvoyager.litememo.domain.usecase.ObserveAppLockEnabledUseCase
import com.appvoyager.litememo.domain.usecase.ObserveMemoSortOrderUseCase
import com.appvoyager.litememo.domain.usecase.ObserveThemeModeUseCase
import com.appvoyager.litememo.domain.usecase.SetAppLockEnabledUseCase
import com.appvoyager.litememo.domain.usecase.SetMemoSortOrderUseCase
import com.appvoyager.litememo.domain.usecase.SetThemeModeUseCase
import com.appvoyager.litememo.ui.auth.AppLockAuthenticationResult
import com.appvoyager.litememo.ui.state.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val observeThemeModeUseCase: ObserveThemeModeUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val observeMemoSortOrderUseCase: ObserveMemoSortOrderUseCase,
    private val setMemoSortOrderUseCase: SetMemoSortOrderUseCase,
    private val observeAppLockEnabledUseCase: ObserveAppLockEnabledUseCase,
    private val setAppLockEnabledUseCase: SetAppLockEnabledUseCase,
    private val exportMemosUseCase: ExportMemosUseCase,
    private val importMemosUseCase: ImportMemosUseCase,
    private val exportFileRepository: ExportFileRepository,
    @param:Named("appVersion") private val appVersion: String
) : ViewModel() {

    private val showThemeDialog = MutableStateFlow(false)
    private val sortOrderExpanded = MutableStateFlow(false)
    private val isExporting = MutableStateFlow(false)
    private val isImporting = MutableStateFlow(false)
    private val showImportConfirmDialog = MutableStateFlow(false)

    private var pendingImportReference: ExportFileReference? = null

    private val _snackbarEvent = Channel<SettingsSnackbarEvent>(Channel.CONFLATED)
    internal val snackbarEvent = _snackbarEvent.receiveAsFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        observeThemeModeUseCase(),
        observeMemoSortOrderUseCase(),
        observeAppLockEnabledUseCase(),
        combine(
            showThemeDialog,
            sortOrderExpanded,
            isExporting,
            isImporting,
            showImportConfirmDialog
        ) { dialog, expanded, exporting, importing, importDialog ->
            UiFlags(dialog, expanded, exporting, importing, importDialog)
        }
    ) { themeMode, sortOrder, appLockEnabled, flags ->
        SettingsUiState(
            themeMode = themeMode,
            memoSortOrder = sortOrder,
            appLockEnabled = appLockEnabled,
            appVersion = appVersion,
            showThemeDialog = flags.showThemeDialog,
            sortOrderExpanded = flags.sortOrderExpanded,
            isExporting = flags.isExporting,
            isImporting = flags.isImporting,
            showImportConfirmDialog = flags.showImportConfirmDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(appVersion = appVersion)
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { runCatching { setThemeModeUseCase(mode) } }
    }

    fun setMemoSortOrder(order: MemoSortOrder) {
        viewModelScope.launch { runCatching { setMemoSortOrderUseCase(order) } }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch { runCatching { setAppLockEnabledUseCase(enabled) } }
    }

    fun onAppLockEnableAuthenticationResult(result: AppLockAuthenticationResult) {
        when (result) {
            AppLockAuthenticationResult.SUCCEEDED -> setAppLockEnabled(true)

            AppLockAuthenticationResult.NO_DEVICE_CREDENTIAL -> {
                _snackbarEvent.trySend(SettingsSnackbarEvent.AppLockNoDeviceCredential)
            }

            AppLockAuthenticationResult.UNAVAILABLE -> {
                _snackbarEvent.trySend(SettingsSnackbarEvent.AppLockUnavailable)
            }

            AppLockAuthenticationResult.FAILED -> {
                _snackbarEvent.trySend(SettingsSnackbarEvent.AppLockAuthenticationFailed)
            }

            AppLockAuthenticationResult.CANCELED -> {
                _snackbarEvent.trySend(SettingsSnackbarEvent.AppLockAuthenticationCanceled)
            }
        }
    }

    fun showThemeDialog() {
        showThemeDialog.value = true
    }

    fun dismissThemeDialog() {
        showThemeDialog.value = false
    }

    fun expandSortOrder() {
        sortOrderExpanded.value = true
    }

    fun collapseSortOrder() {
        sortOrderExpanded.value = false
    }

    fun exportMemos(reference: ExportFileReference) {
        if (isExporting.value || isImporting.value) return
        viewModelScope.launch {
            isExporting.value = true
            try {
                val exportData = exportMemosUseCase()
                exportFileRepository.write(reference, exportData)
                _snackbarEvent.trySend(SettingsSnackbarEvent.ExportSuccess)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                _snackbarEvent.trySend(SettingsSnackbarEvent.ExportError)
            } finally {
                isExporting.value = false
            }
        }
    }

    fun onImportFileSelected(reference: ExportFileReference) {
        if (isExporting.value || isImporting.value) return
        pendingImportReference = reference
        showImportConfirmDialog.value = true
    }

    fun confirmImport() {
        if (isImporting.value || isExporting.value) return
        val reference = pendingImportReference ?: return
        showImportConfirmDialog.value = false
        pendingImportReference = null
        viewModelScope.launch {
            isImporting.value = true
            try {
                val exportData = exportFileRepository.read(reference)
                importMemosUseCase(exportData)
                _snackbarEvent.trySend(SettingsSnackbarEvent.ImportSuccess)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                _snackbarEvent.trySend(SettingsSnackbarEvent.ImportError)
            } finally {
                isImporting.value = false
            }
        }
    }

    fun dismissImportConfirmDialog() {
        showImportConfirmDialog.value = false
        pendingImportReference = null
    }
}

internal sealed interface SettingsSnackbarEvent {
    data object ExportSuccess : SettingsSnackbarEvent
    data object ExportError : SettingsSnackbarEvent
    data object ImportSuccess : SettingsSnackbarEvent
    data object ImportError : SettingsSnackbarEvent
    data object AppLockAuthenticationFailed : SettingsSnackbarEvent
    data object AppLockAuthenticationCanceled : SettingsSnackbarEvent
    data object AppLockNoDeviceCredential : SettingsSnackbarEvent
    data object AppLockUnavailable : SettingsSnackbarEvent
}

private data class UiFlags(
    val showThemeDialog: Boolean,
    val sortOrderExpanded: Boolean,
    val isExporting: Boolean,
    val isImporting: Boolean,
    val showImportConfirmDialog: Boolean
)
