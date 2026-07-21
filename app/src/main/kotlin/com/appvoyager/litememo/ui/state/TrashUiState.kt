package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.ui.model.TrashedMemoUiModel

data class TrashUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val memos: List<TrashedMemoUiModel> = emptyList(),
    val selection: TrashSelectionUiState = TrashSelectionUiState(),
    val showEmptyTrashDialog: Boolean = false
)

data class TrashSelectionUiState(val selectedMemoIds: Set<MemoId> = emptySet()) {

    val isActive: Boolean
        get() = selectedMemoIds.isNotEmpty()

    val selectedCount: Int
        get() = selectedMemoIds.size

    fun contains(memoId: MemoId): Boolean = memoId in selectedMemoIds
}
