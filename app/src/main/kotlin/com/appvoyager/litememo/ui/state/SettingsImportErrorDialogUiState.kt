package com.appvoyager.litememo.ui.state

sealed interface SettingsImportErrorDialogUiState {

    data class TagNameConflict(val tagNames: List<String>) : SettingsImportErrorDialogUiState

    data object Generic : SettingsImportErrorDialogUiState
}
