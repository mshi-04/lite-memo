package com.appvoyager.litememo.ui.state

data class HomeUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val errorCause: Throwable? = null,
    val selectedFilter: HomeFilterUiState = HomeFilterUiState.All,
    val summary: HomeSummaryUiState = HomeSummaryUiState(),
    val memos: List<HomeMemoUiModel> = emptyList()
)
