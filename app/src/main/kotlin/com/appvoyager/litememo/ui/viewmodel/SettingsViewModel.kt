package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.litememo.di.AppVersion
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

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeThemeModeUseCase: ObserveThemeModeUseCase,
    observeMemoSortOrderUseCase: ObserveMemoSortOrderUseCase,
    observeAppLockEnabledUseCase: ObserveAppLockEnabledUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val setMemoSortOrderUseCase: SetMemoSortOrderUseCase,
    private val setAppLockEnabledUseCase: SetAppLockEnabledUseCase,
    private val exportMemosUseCase: ExportMemosUseCase,
    private val importMemosUseCase: ImportMemosUseCase,
    private val exportFileRepository: ExportFileRepository,
    @param:AppVersion private val appVersion: String
) : ViewModel() {

    private val themeDropdownExpanded = MutableStateFlow(false)
    private val sortOrderExpanded = MutableStateFlow(false)
    private val isExporting = MutableStateFlow(false)
    private val isImporting = MutableStateFlow(false)
    private val showImportConfirmDialog = MutableStateFlow(false)
    private var pendingImportReference: ExportFileReference? = null
    private var isAppLockAuthenticating = false

    private val _snackbarEvent = Channel<SettingsSnackbarEvent>(Channel.BUFFERED)
    internal val snackbarEvent = _snackbarEvent.receiveAsFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        observeThemeModeUseCase(),
        observeMemoSortOrderUseCase(),
        observeAppLockEnabledUseCase(),
        combine(
            themeDropdownExpanded,
            sortOrderExpanded,
            isExporting,
            isImporting,
            showImportConfirmDialog
        ) { themeExpanded, expanded, exporting, importing, importDialog ->
            UiFlags(themeExpanded, expanded, exporting, importing, importDialog)
        }
    ) { themeMode, sortOrder, appLockEnabled, flags ->
        SettingsUiState(
            themeMode = themeMode,
            memoSortOrder = sortOrder,
            appLockEnabled = appLockEnabled,
            appVersion = appVersion,
            themeDropdownExpanded = flags.themeDropdownExpanded,
            sortOrderExpanded = flags.sortOrderExpanded,
            isExporting = flags.isExporting,
            isImporting = flags.isImporting,
            showImportConfirmDialog = flags.showImportConfirmDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
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

    /** 認証要求が受け付けられたら true。認証中の重複要求は false を返して無視させる。 */
    fun beginAppLockAuthentication(): Boolean {
        if (isAppLockAuthenticating) return false
        isAppLockAuthenticating = true
        return true
    }

    fun onAppLockEnableAuthenticationResult(result: AppLockAuthenticationResult) {
        isAppLockAuthenticating = false
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

    fun expandThemeDropdown() {
        sortOrderExpanded.value = false
        themeDropdownExpanded.value = true
    }

    fun collapseThemeDropdown() {
        themeDropdownExpanded.value = false
    }

    fun expandSortOrder() {
        themeDropdownExpanded.value = false
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

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
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
    val themeDropdownExpanded: Boolean,
    val sortOrderExpanded: Boolean,
    val isExporting: Boolean,
    val isImporting: Boolean,
    val showImportConfirmDialog: Boolean
)
