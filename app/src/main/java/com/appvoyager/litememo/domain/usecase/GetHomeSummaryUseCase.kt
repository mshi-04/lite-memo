package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.HomeSummary
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.provider.CurrentTimeProvider
import java.time.Instant
import java.time.ZoneId

class GetHomeSummaryUseCase(
    private val currentTimeProvider: CurrentTimeProvider,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {

    operator fun invoke(memos: List<Memo>): HomeSummary {
        val today = currentTimeProvider.now().toLocalDate()

        return HomeSummary(
            totalCount = memos.size,
            todayCount = memos.count {
                it.createdAt.toLocalDate() == today ||
                    it.updatedAt.toLocalDate() == today
            },
            unorganizedCount = memos.count { it.tagIds.isEmpty() },
            importantCount = memos.count { it.isImportant }
        )
    }

    private fun TimestampMillis.toLocalDate() =
        Instant.ofEpochMilli(value).atZone(zoneId).toLocalDate()
    
}
