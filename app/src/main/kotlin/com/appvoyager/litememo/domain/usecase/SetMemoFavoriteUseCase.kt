package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.provider.CurrentTimeProvider
import com.appvoyager.litememo.domain.repository.MemoRepository
import javax.inject.Inject

class SetMemoFavoriteUseCase @Inject constructor(
    private val memoRepository: MemoRepository,
    private val currentTimeProvider: CurrentTimeProvider
) {

    suspend operator fun invoke(id: MemoId, isFavorite: Boolean): Memo {
        val memo = requireNotNull(memoRepository.getActiveMemo(id)) {
            "Memo not found: ${id.value}"
        }
        if (memo.isFavorite == isFavorite) return memo

        val now = currentTimeProvider.now()
        val updatedMemo = memo.copy(
            updatedAt = TimestampMillis(maxOf(now.value, memo.createdAt.value)),
            isFavorite = isFavorite
        )

        memoRepository.saveMemo(updatedMemo)
        return updatedMemo
    }

}
