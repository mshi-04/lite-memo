package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.provider.CurrentTimeProvider
import com.appvoyager.litememo.domain.repository.MemoRepository
import javax.inject.Inject

class PurgeExpiredTrashedMemosUseCase @Inject constructor(
    private val memoRepository: MemoRepository,
    private val currentTimeProvider: CurrentTimeProvider
) {

    suspend operator fun invoke() {
        val cutoff = TimestampMillis(currentTimeProvider.now().value - RETENTION_MILLIS)
        memoRepository.deleteTrashedMemosDeletedAtOrBefore(cutoff)
    }

    private companion object {
        const val RETENTION_MILLIS = 30L * 24L * 60L * 60L * 1_000L
    }

}
