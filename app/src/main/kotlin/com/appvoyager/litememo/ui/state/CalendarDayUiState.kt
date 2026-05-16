package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.model.CalendarDate

data class CalendarDayUiState(
    val date: CalendarDate,
    val dayOfMonth: Int,
    val isSelected: Boolean,
    val hasMemo: Boolean
)
