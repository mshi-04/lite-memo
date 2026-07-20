package com.appvoyager.litememo.ui.event

sealed interface SettingsSnackbarUiEvent {
    data object ExportSuccess : SettingsSnackbarUiEvent
    data object ExportError : SettingsSnackbarUiEvent
    data object ImportSuccess : SettingsSnackbarUiEvent
    data object AppLockAuthenticationFailed : SettingsSnackbarUiEvent
    data object AppLockAuthenticationCanceled : SettingsSnackbarUiEvent
    data object AppLockNoDeviceCredential : SettingsSnackbarUiEvent
    data object AppLockUnavailable : SettingsSnackbarUiEvent
}
