package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.ui.model.MemoUiModel
import com.appvoyager.litememo.ui.model.TagUiModel

data class HomeUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val selectedFilter: HomeFilterUiState = HomeFilterUiState.All,
    val search: SearchUiState = SearchUiState(),
    val selection: HomeSelectionUiState = HomeSelectionUiState(),
    val allSelectedFavorite: Boolean = false,
    val allSelectedTagIds: Set<TagId> = emptySet(),
    val bulkTagDialog: HomeBulkTagDialogUiState = HomeBulkTagDialogUiState(),
    val tags: List<TagUiModel> = emptyList(),
    val memos: List<MemoUiModel> = emptyList()
)
