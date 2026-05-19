package com.appvoyager.litememo.ui.state

data class MemoEditUiState(
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val memoId: String? = null,
    val title: String = "",
    val body: String = "",
    val isImportant: Boolean = false,
    val isModified: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val availableTags: List<TagUiModel> = emptyList(),
    val selectedTagIds: Set<String> = emptySet()
)
