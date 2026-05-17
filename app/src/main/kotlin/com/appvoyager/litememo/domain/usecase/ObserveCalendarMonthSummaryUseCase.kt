package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.CalendarDate
import com.appvoyager.litememo.domain.model.CalendarDaySummary
import com.appvoyager.litememo.domain.model.CalendarMonth
import com.appvoyager.litememo.domain.model.CalendarMonthSummary
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.repository.MemoRepository
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveCalendarMonthSummaryUseCase @Inject constructor(
    private val memoRepository: MemoRepository,
    private val zoneId: ZoneId
) {

    operator fun invoke(month: CalendarMonth): Flow<CalendarMonthSummary> {
        val from = TimestampMillis(
            month.value.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        )
        val to = TimestampMillis(
            month.value.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        )
        return memoRepository.observeMemosCreatedBetween(from, to).map { memos ->
            val countsByDate = memos
                .groupingBy { memo -> CalendarDate.from(memo.createdAt, zoneId) }
                .eachCount()

            CalendarMonthSummary(
                month = month,
                days = month.toCalendarDates().map { date ->
                    CalendarDaySummary(
                        date = date,
                        memoCount = countsByDate[date] ?: 0
                    )
                }
            )
        }
    }

}
