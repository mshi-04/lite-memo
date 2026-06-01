package com.appvoyager.litememo.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarMonthTest {

    @Test
    fun toCalendarDatesReturnsTwentyNineDaysForLeapFebruary() {
        // Arrange
        val month = CalendarMonth(YearMonth.of(2024, 2))

        // Act
        val dates = month.toCalendarDates()

        // Assert
        assertEquals(29, dates.size)
    }

    @Test
    fun toCalendarDatesReturnsTwentyEightDaysForNonLeapFebruary() {
        // Arrange
        val month = CalendarMonth(YearMonth.of(2025, 2))

        // Act
        val dates = month.toCalendarDates()

        // Assert
        assertEquals(28, dates.size)
    }

    @Test
    fun toCalendarDatesReturnsAscendingDatesFromFirstDayOfMonth() {
        // Arrange
        val month = CalendarMonth(YearMonth.of(2026, 4))

        // Act
        val dates = month.toCalendarDates()

        // Assert
        assertEquals(
            (1..30).map { day -> CalendarDate(LocalDate.of(2026, 4, day)) },
            dates
        )
    }
}
