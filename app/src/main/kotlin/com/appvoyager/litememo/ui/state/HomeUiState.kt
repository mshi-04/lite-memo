package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.model.value.TagId

data class HomeUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val selectedFilter: HomeFilterUiState = HomeFilterUiState.All,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val hasSearchError: Boolean = false,
    val selection: HomeSelectionUiState = HomeSelectionUiState(),
    val allSelectedFavorite: Boolean = false,
    val allSelectedTagIds: Set<TagId> = emptySet(),
    val bulkTagDialog: HomeBulkTagDialogUiState = HomeBulkTagDialogUiState(),
    val tags: List<TagUiModel> = emptyList(),
    val memos: List<MemoUiModel> = emptyList(),
    val searchResults: List<MemoUiModel> = emptyList()
)
