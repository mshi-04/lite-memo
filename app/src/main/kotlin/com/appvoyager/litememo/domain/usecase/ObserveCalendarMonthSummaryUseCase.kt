package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.CalendarDate
import com.appvoyager.litememo.domain.model.CalendarDaySummary
import com.appvoyager.litememo.domain.model.CalendarMonth
import com.appvoyager.litememo.domain.model.CalendarMonthSummary
import com.appvoyager.litememo.domain.repository.MemoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.ZoneId
import javax.inject.Inject

class ObserveCalendarMonthSummaryUseCase @Inject constructor(
    private val memoRepository: MemoRepository,
    private val zoneId: ZoneId
) {

    operator fun invoke(month: CalendarMonth): Flow<CalendarMonthSummary> =
        memoRepository.observeActiveMemosCreatedBetween(
            month.toTimestampRange(zoneId)
        ).map { memos ->
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
