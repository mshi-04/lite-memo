package com.appvoyager.litememo.domain.model

import com.appvoyager.litememo.domain.model.value.TimestampMillis
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@JvmInline
value class CalendarDate(val value: LocalDate) {

    companion object {
        fun from(timestamp: TimestampMillis, zoneId: ZoneId): CalendarDate =
            CalendarDate(Instant.ofEpochMilli(timestamp.value).atZone(zoneId).toLocalDate())
    }

}
