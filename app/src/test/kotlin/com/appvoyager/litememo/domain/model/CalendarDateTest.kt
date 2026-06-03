package com.appvoyager.litememo.domain.model

import com.appvoyager.litememo.domain.model.value.TimestampMillis
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class CalendarDateTest {

    @Test
    fun fromMapsTimestampToLocalDateInUtc() {
        // Arrange
        val timestamp = TimestampMillis(Instant.parse("2026-05-31T15:00:00Z").toEpochMilli())

        // Act
        val date = CalendarDate.from(timestamp, ZoneId.of("UTC"))

        // Assert
        assertEquals(CalendarDate(LocalDate.of(2026, 5, 31)), date)
    }

    @Test
    fun fromMapsTimestampToNextDayAtTokyoZoneBoundary() {
        // Arrange
        val timestamp = TimestampMillis(Instant.parse("2026-05-31T15:00:00Z").toEpochMilli())

        // Act
        val date = CalendarDate.from(timestamp, ZoneId.of("Asia/Tokyo"))

        // Assert
        assertEquals(CalendarDate(LocalDate.of(2026, 6, 1)), date)
    }
}
