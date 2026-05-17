package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.CalendarDate
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.MemoSortOrder
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.repository.UserSettingsRepository
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveMemosByCalendarDateUseCase @Inject constructor(
    private val memoRepository: MemoRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val zoneId: ZoneId
) {

    operator fun invoke(date: CalendarDate): Flow<List<Memo>> {
        val from = TimestampMillis(
            date.value.atStartOfDay(zoneId).toInstant().toEpochMilli()
        )
        val to = TimestampMillis(
            date.value.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        )
        return combine(
            memoRepository.observeMemosCreatedBetween(from, to),
            userSettingsRepository.observeMemoSortOrder()
        ) { memos, sortOrder ->
            when (sortOrder) {
                MemoSortOrder.UPDATED_NEWEST -> memos.sortedWith(
                    compareByDescending<Memo> { it.updatedAt.value }
                        .thenByDescending { it.createdAt.value }
                )

                MemoSortOrder.CREATED_NEWEST -> memos.sortedWith(
                    compareByDescending<Memo> { it.createdAt.value }
                        .thenByDescending { it.updatedAt.value }
                )
            }
        }
    }

}
