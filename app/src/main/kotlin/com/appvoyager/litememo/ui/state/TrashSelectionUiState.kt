package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.model.value.MemoId

data class TrashSelectionUiState(val selectedMemoIds: Set<MemoId> = emptySet()) {
    val isActive: Boolean
        get() = selectedMemoIds.isNotEmpty()

    val selectedCount: Int
        get() = selectedMemoIds.size

    fun contains(memoId: MemoId): Boolean = memoId in selectedMemoIds
}
