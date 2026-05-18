package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.model.MemoSortOrder
import com.appvoyager.litememo.domain.model.ThemeMode

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val memoSortOrder: MemoSortOrder = MemoSortOrder.UPDATED_NEWEST,
    val appVersion: String = "",
    val showThemeDialog: Boolean = false,
    val sortOrderExpanded: Boolean = false
)
