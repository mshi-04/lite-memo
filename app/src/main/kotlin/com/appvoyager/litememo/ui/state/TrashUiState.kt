package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.TimestampMillis

data class TrashUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val memos: List<TrashedMemoUiModel> = emptyList(),
    val showPermanentDeleteDialog: TrashedMemoUiModel? = null
)

data class TrashedMemoUiModel(
    val id: MemoId,
    val title: String,
    val body: String,
    val tags: List<TagUiModel>,
    val deletedAt: TimestampMillis
)
