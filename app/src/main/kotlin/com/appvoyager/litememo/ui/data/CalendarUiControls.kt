package com.appvoyager.litememo.ui.data

import com.appvoyager.litememo.ui.state.SearchUiState

data class CalendarUiControls(
    val expanded: Boolean,
    val datePickerVisible: Boolean,
    val search: SearchUiState
)
