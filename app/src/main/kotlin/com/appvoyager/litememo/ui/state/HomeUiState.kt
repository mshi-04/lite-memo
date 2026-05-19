package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.model.MemoSortOrder

data class HomeUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val selectedFilter: HomeFilterUiState = HomeFilterUiState.All,
    val memoSortOrder: MemoSortOrder = MemoSortOrder.UPDATED_NEWEST,
    val summary: HomeSummaryUiState = HomeSummaryUiState(),
    val memos: List<MemoUiModel> = emptyList()
)
