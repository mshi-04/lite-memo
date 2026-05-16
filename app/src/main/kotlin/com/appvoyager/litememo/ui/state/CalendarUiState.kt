package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.model.CalendarDate
import com.appvoyager.litememo.domain.model.CalendarMonth

data class CalendarUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val selectedMonth: CalendarMonth? = null,
    val selectedDate: CalendarDate? = null,
    val isCalendarExpanded: Boolean = true,
    val isDatePickerVisible: Boolean = false,
    val days: List<CalendarDayUiState> = emptyList(),
    val memos: List<CalendarMemoUiModel> = emptyList()
)
