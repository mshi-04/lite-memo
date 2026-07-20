package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.ui.model.TagUiModel

data class TagManageUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val tags: List<TagUiModel> = emptyList(),
    val editingTag: TagEditUiState? = null,
    val showDeleteDialog: TagUiModel? = null
)
