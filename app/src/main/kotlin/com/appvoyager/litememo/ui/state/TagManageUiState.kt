package com.appvoyager.litememo.ui.state

data class TagManageUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val hasDeleteError: Boolean = false,
    val tags: List<TagUiModel> = emptyList(),
    val editingTag: TagEditState? = null,
    val showDeleteDialog: TagUiModel? = null
)

data class TagEditState(
    val id: String? = null,
    val name: String = "",
    val colorArgb: Long = DEFAULT_TAG_COLORS.first(),
    val nameError: Boolean = false,
    val saveError: Boolean = false
)

val DEFAULT_TAG_COLORS: List<Long> = listOf(
    0xFF6750A4,
    0xFFB3261E,
    0xFF006D3B,
    0xFF0061A4,
    0xFF7D5260,
    0xFF984061,
    0xFF006874,
    0xFF795548,
    0xFF5C6BC0,
    0xFFE65100
)
