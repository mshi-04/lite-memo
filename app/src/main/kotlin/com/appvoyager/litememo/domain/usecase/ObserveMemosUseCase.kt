package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.sortedBy
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
        memoRepository.observeActiveMemos(),
        userSettingsRepository.observeMemoSortOrder()
    ) { memos, sortOrder ->
        memos.sortedBy(sortOrder)
    }

}
