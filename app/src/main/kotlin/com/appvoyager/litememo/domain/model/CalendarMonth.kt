package com.appvoyager.litememo.domain.model

import java.time.YearMonth

@JvmInline
value class CalendarMonth(val value: YearMonth) {

    fun dates(): List<CalendarDate> = (1..value.lengthOfMonth()).map { dayOfMonth ->
        CalendarDate(value.atDay(dayOfMonth))
    }

}
