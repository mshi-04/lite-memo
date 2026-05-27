package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.model.MemoSortOrder

data class HomeUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val hasActionError: Boolean = false,
    val selectedFilter: HomeFilterUiState = HomeFilterUiState.All,
    val memoSortOrder: MemoSortOrder = MemoSortOrder.UPDATED_NEWEST,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val hasSearchError: Boolean = false,
    val selection: HomeSelectionUiState = HomeSelectionUiState(),
    val bulkTagDialog: HomeBulkTagDialogUiState = HomeBulkTagDialogUiState(),
    val tags: List<TagUiModel> = emptyList(),
    val summary: HomeSummaryUiState = HomeSummaryUiState(),
    val memos: List<MemoUiModel> = emptyList(),
    val searchResults: List<MemoUiModel> = emptyList()
)
