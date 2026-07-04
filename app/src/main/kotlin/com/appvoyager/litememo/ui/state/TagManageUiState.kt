package com.appvoyager.litememo.ui.state

data class TagManageUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val tags: List<TagUiModel> = emptyList(),
    val editingTag: TagEditState? = null,
    val showDeleteDialog: TagUiModel? = null
)

data class TagEditState(
    val id: String? = null,
    val name: String = "",
    val colorArgb: Long = DEFAULT_TAG_COLORS.first(),
    val nameError: Boolean = false,
    val duplicateNameError: Boolean = false,
    val saveError: Boolean = false,
    val isSaving: Boolean = false
)
