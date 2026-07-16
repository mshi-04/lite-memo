package com.appvoyager.litememo.ui.state

import java.time.YearMonth

data class CalendarGridAnimationState(val month: YearMonth?, val days: List<CalendarDayUiState>)
