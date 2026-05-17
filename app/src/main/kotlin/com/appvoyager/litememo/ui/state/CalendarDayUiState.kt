package com.appvoyager.litememo.ui.state

import java.time.LocalDate

data class CalendarDayUiState(
    val date: LocalDate,
    val dayOfMonth: Int,
    val isSelected: Boolean,
    val hasMemo: Boolean
)
