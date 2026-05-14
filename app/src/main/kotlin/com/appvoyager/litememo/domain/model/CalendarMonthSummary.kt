package com.appvoyager.litememo.domain.model

import java.time.YearMonth

data class CalendarMonthSummary(val month: CalendarMonth, val days: List<CalendarDaySummary>) {

    init {
        require(days.all { YearMonth.from(it.date.value) == month.value }) {
            "CalendarMonthSummary days must belong to the summary month."
        }
    }

}
