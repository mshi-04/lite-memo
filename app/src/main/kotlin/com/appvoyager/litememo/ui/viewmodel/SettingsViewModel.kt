package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.litememo.di.AppVersion
import com.appvoyager.litememo.domain.exception.ImportTagNameConflictException
import com.appvoyager.litememo.domain.model.MemoSortOrder
import com.appvoyager.litememo.domain.model.ThemeMode
import com.appvoyager.litememo.domain.model.value.ExportFileReference
import com.appvoyager.litememo.domain.usecase.ExportMemosToFileUseCase
import com.appvoyager.litememo.domain.usecase.ImportMemosFromFileUseCase
import com.appvoyager.litememo.domain.usecase.ObserveAppLockEnabledUseCase
import com.appvoyager.litememo.domain.usecase.ObserveMemoSortOrderUseCase
import com.appvoyager.litememo.domain.usecase.ObserveThemeModeUseCase
import com.appvoyager.litememo.domain.usecase.SetAppLockEnabledUseCase
import com.appvoyager.litememo.domain.usecase.SetMemoSortOrderUseCase
import com.appvoyager.litememo.domain.usecase.SetThemeModeUseCase
import com.appvoyager.litememo.ui.data.SettingsUiFlags
import com.appvoyager.litememo.ui.event.SettingsSnackbarUiEvent
import com.appvoyager.litememo.ui.state.SettingsImportErrorDialogUiState
import com.appvoyager.litememo.ui.state.SettingsUiState
import com.appvoyager.litememo.ui.type.AppLockAuthenticationUiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
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
    private val exportMemosToFileUseCase: ExportMemosToFileUseCase,
    private val importMemosFromFileUseCase: ImportMemosFromFileUseCase,
    @param:AppVersion private val appVersion: String
) : ViewModel() {

    private val themeDropdownExpanded = MutableStateFlow(false)
    private val sortOrderExpanded = MutableStateFlow(false)
    private val isExporting = MutableStateFlow(false)
    private val isImporting = MutableStateFlow(false)
    private val showImportConfirmDialog = MutableStateFlow(false)
    private val importErrorDialog = MutableStateFlow<SettingsImportErrorDialogUiState?>(null)
    private var pendingImportReference: ExportFileReference? = null
    private var isAppLockAuthenticating = false

    private val _snackbarEvent = Channel<SettingsSnackbarUiEvent>(Channel.BUFFERED)
    val snackbarEvent: Flow<SettingsSnackbarUiEvent> = _snackbarEvent.receiveAsFlow()

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
            SettingsUiFlags(themeExpanded, expanded, exporting, importing, importDialog)
        },
        importErrorDialog
    ) { themeMode, sortOrder, appLockEnabled, flags, importError ->
        SettingsUiState(
            themeMode = themeMode,
            memoSortOrder = sortOrder,
            appLockEnabled = appLockEnabled,
            appVersion = appVersion,
            themeDropdownExpanded = flags.themeDropdownExpanded,
            sortOrderExpanded = flags.sortOrderExpanded,
            isExporting = flags.isExporting,
            isImporting = flags.isImporting,
            showImportConfirmDialog = flags.showImportConfirmDialog,
            importErrorDialog = importError
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

    // 認証要求が受け付けられたら true。認証中の重複要求は false を返して無視させる。
    fun beginAppLockAuthentication(): Boolean {
        if (isAppLockAuthenticating) return false
        isAppLockAuthenticating = true
        return true
    }

    fun onAppLockEnableAuthenticationResult(result: AppLockAuthenticationUiResult) {
        isAppLockAuthenticating = false
        when (result) {
            AppLockAuthenticationUiResult.SUCCEEDED -> setAppLockEnabled(true)

            AppLockAuthenticationUiResult.NO_DEVICE_CREDENTIAL -> {
                _snackbarEvent.trySend(SettingsSnackbarUiEvent.AppLockNoDeviceCredential)
            }

            AppLockAuthenticationUiResult.UNAVAILABLE -> {
                _snackbarEvent.trySend(SettingsSnackbarUiEvent.AppLockUnavailable)
            }

            AppLockAuthenticationUiResult.FAILED -> {
                _snackbarEvent.trySend(SettingsSnackbarUiEvent.AppLockAuthenticationFailed)
            }

            AppLockAuthenticationUiResult.CANCELED -> {
                _snackbarEvent.trySend(SettingsSnackbarUiEvent.AppLockAuthenticationCanceled)
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
                exportMemosToFileUseCase(reference)
                _snackbarEvent.trySend(SettingsSnackbarUiEvent.ExportSuccess)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                _snackbarEvent.trySend(SettingsSnackbarUiEvent.ExportError)
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
                importMemosFromFileUseCase(reference)
                _snackbarEvent.trySend(SettingsSnackbarUiEvent.ImportSuccess)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ImportTagNameConflictException) {
                importErrorDialog.value = SettingsImportErrorDialogUiState.TagNameConflict(
                    tagNames = e.tagNames.map { it.value }
                )
            } catch (_: Throwable) {
                importErrorDialog.value = SettingsImportErrorDialogUiState.Generic
            } finally {
                isImporting.value = false
            }
        }
    }

    fun dismissImportConfirmDialog() {
        showImportConfirmDialog.value = false
        pendingImportReference = null
    }

    fun dismissImportErrorDialog() {
        importErrorDialog.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
