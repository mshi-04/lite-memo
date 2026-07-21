package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.ui.model.TagUiModel
import com.appvoyager.litememo.ui.theme.DEFAULT_TAG_COLORS

data class TagManageUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val tags: List<TagUiModel> = emptyList(),
    val editingTag: TagEditUiState? = null,
    val showDeleteDialog: TagUiModel? = null
)

data class TagEditUiState(
    val id: TagId? = null,
    val name: String = "",
    val colorArgb: Long = DEFAULT_TAG_COLORS.first().argb,
    val nameError: Boolean = false,
    val duplicateNameError: Boolean = false,
    val saveError: Boolean = false,
    val isSaving: Boolean = false
)
