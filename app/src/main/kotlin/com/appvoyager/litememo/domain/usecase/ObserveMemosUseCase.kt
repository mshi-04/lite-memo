package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.MemoSortOrder
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.repository.UserSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveMemosUseCase @Inject constructor(
    private val memoRepository: MemoRepository,
    private val userSettingsRepository: UserSettingsRepository
) {

    operator fun invoke(): Flow<List<Memo>> = combine(
        memoRepository.observeMemos(),
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
