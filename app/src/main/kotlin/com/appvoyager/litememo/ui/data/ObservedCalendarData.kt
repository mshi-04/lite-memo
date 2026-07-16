package com.appvoyager.litememo.ui.data

import com.appvoyager.litememo.domain.model.CalendarMonthSummary
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.Tag

data class ObservedCalendarData(
    val monthSummary: CalendarMonthSummary?,
    val memos: List<Memo>?,
    val tags: List<Tag>?
)
