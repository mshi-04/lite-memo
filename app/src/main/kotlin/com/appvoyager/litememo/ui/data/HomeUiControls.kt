package com.appvoyager.litememo.ui.data

import com.appvoyager.litememo.ui.state.HomeBulkTagDialogUiState
import com.appvoyager.litememo.ui.state.HomeFilterUiState
import com.appvoyager.litememo.ui.state.HomeSelectionUiState
import com.appvoyager.litememo.ui.state.SearchUiState

data class HomeUiControls(
    val filter: HomeFilterUiState,
    val search: SearchUiState,
    val selection: HomeSelectionUiState,
    val tagDialog: HomeBulkTagDialogUiState
)
