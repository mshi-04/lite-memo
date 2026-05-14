package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.CalendarDate
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.repository.MemoRepository
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveMemosByCalendarDateUseCase @Inject constructor(
    private val memoRepository: MemoRepository,
    private val zoneId: ZoneId
) {

    operator fun invoke(date: CalendarDate): Flow<List<Memo>> =
        memoRepository.observeMemos().map { memos ->
            memos
                .filter { memo -> memo.createdAt.toCalendarDate() == date }
                .sortedWith(
                    compareByDescending<Memo> { memo -> memo.updatedAt.value }
                        .thenByDescending { memo -> memo.createdAt.value }
                )
        }

    private fun TimestampMillis.toCalendarDate() =
        CalendarDate(Instant.ofEpochMilli(value).atZone(zoneId).toLocalDate())

}
