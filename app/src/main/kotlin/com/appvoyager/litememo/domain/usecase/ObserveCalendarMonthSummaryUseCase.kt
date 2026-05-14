package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.CalendarDate
import com.appvoyager.litememo.domain.model.CalendarDaySummary
import com.appvoyager.litememo.domain.model.CalendarMonth
import com.appvoyager.litememo.domain.model.CalendarMonthSummary
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.repository.MemoRepository
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveCalendarMonthSummaryUseCase @Inject constructor(
    private val memoRepository: MemoRepository,
    private val zoneId: ZoneId
) {

    operator fun invoke(month: CalendarMonth): Flow<CalendarMonthSummary> =
        memoRepository.observeMemos().map { memos ->
            val countsByDate = memos
                .groupingBy { memo -> memo.createdAt.toCalendarDate() }
                .eachCount()

            CalendarMonthSummary(
                month = month,
                days = month.dates().map { date ->
                    CalendarDaySummary(
                        date = date,
                        memoCount = countsByDate[date] ?: 0
                    )
                }
            )
        }

    private fun TimestampMillis.toCalendarDate() =
        CalendarDate(Instant.ofEpochMilli(value).atZone(zoneId).toLocalDate())

}
