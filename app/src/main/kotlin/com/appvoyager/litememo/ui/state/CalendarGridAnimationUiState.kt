package com.appvoyager.litememo.ui.state

import java.time.YearMonth

data class CalendarGridAnimationUiState(val month: YearMonth?, val days: List<CalendarDayUiState>)
