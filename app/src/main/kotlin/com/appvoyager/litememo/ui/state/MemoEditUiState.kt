package com.appvoyager.litememo.ui.state

data class MemoEditUiState(
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val memoId: String? = null,
    val title: String = "",
    val body: String = "",
    val isFavorite: Boolean = false,
    val isModified: Boolean = false,
    val isSaving: Boolean = false,
    val isDeletePending: Boolean = false,
    val availableTags: List<TagUiModel> = emptyList(),
    val selectedTagIds: Set<String> = emptySet()
)
