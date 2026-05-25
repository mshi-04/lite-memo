package com.appvoyager.litememo.ui.state

data class TrashUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val hasActionError: Boolean = false,
    val memos: List<TrashedMemoUiModel> = emptyList(),
    val showPermanentDeleteDialog: TrashedMemoUiModel? = null
)

data class TrashedMemoUiModel(
    val id: String,
    val title: String,
    val body: String,
    val tags: List<TagUiModel>,
    val deletedAtMillis: Long
)
