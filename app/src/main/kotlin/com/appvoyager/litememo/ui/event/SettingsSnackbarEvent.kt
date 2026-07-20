package com.appvoyager.litememo.ui.event

sealed interface SettingsSnackbarEvent {
    data object ExportSuccess : SettingsSnackbarEvent
    data object ExportError : SettingsSnackbarEvent
    data object ImportSuccess : SettingsSnackbarEvent
    data object AppLockAuthenticationFailed : SettingsSnackbarEvent
    data object AppLockAuthenticationCanceled : SettingsSnackbarEvent
    data object AppLockNoDeviceCredential : SettingsSnackbarEvent
    data object AppLockUnavailable : SettingsSnackbarEvent
}
