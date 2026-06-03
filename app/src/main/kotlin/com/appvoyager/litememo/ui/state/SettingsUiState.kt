package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.model.MemoSortOrder
import com.appvoyager.litememo.domain.model.ThemeMode

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val memoSortOrder: MemoSortOrder = MemoSortOrder.UPDATED_NEWEST,
    val appLockEnabled: Boolean = false,
    val appVersion: String = "",
    val themeDropdownExpanded: Boolean = false,
    val sortOrderExpanded: Boolean = false,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val showImportConfirmDialog: Boolean = false
)
