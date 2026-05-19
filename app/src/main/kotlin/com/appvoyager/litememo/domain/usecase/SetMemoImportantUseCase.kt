package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.provider.CurrentTimeProvider
import com.appvoyager.litememo.domain.repository.MemoRepository
import javax.inject.Inject

class SetMemoImportantUseCase @Inject constructor(
    private val memoRepository: MemoRepository,
    private val currentTimeProvider: CurrentTimeProvider
) {

    suspend operator fun invoke(id: MemoId, isImportant: Boolean): Memo {
        val memo = requireNotNull(memoRepository.getMemo(id)) {
            "Memo not found: ${id.value}"
        }
        val updatedMemo = memo.copy(
            updatedAt = currentTimeProvider.now(),
            isImportant = isImportant
        )

        memoRepository.saveMemo(updatedMemo)
        return updatedMemo
    }

}
