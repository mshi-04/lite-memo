package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.ui.model.TrashedMemoUiModel

data class TrashUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val memos: List<TrashedMemoUiModel> = emptyList(),
    val selection: TrashSelectionUiState = TrashSelectionUiState(),
    val showEmptyTrashDialog: Boolean = false
)
