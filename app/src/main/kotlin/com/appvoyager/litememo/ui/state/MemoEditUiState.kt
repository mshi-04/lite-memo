package com.appvoyager.litememo.ui.state

data class MemoEditUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val memoId: String? = null,
    val title: String = "",
    val body: String = "",
    val isModified: Boolean = false,
    val showDiscardDialog: Boolean = false
)
