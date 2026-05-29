package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.provider.CurrentTimeProvider
import com.appvoyager.litememo.domain.repository.MemoRepository
import javax.inject.Inject

class MoveMemoToTrashUseCase @Inject constructor(
    private val memoRepository: MemoRepository,
    private val currentTimeProvider: CurrentTimeProvider
) {

    suspend operator fun invoke(id: MemoId): MemoId {
        val memo = requireNotNull(memoRepository.getActiveMemo(id)) {
            "Memo not found: ${id.value}"
        }
        val now = currentTimeProvider.now()
        val deletedAt = TimestampMillis(maxOf(now.value, memo.createdAt.value))
        memoRepository.moveMemoToTrash(id, deletedAt)
        return id
    }

}
