package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.ui.state.CalendarDayUiState
import java.time.YearMonth

data class CalendarGridAnimationState(val month: YearMonth?, val days: List<CalendarDayUiState>)
