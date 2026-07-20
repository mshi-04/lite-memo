package com.appvoyager.litememo.ui.state

sealed interface SettingsImportErrorDialogState {

    data class TagNameConflict(val tagNames: List<String>) : SettingsImportErrorDialogState

    data object Generic : SettingsImportErrorDialogState
}
