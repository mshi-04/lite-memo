package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.sortedBy
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.repository.UserSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class SearchMemosUseCase @Inject constructor(
    private val memoRepository: MemoRepository,
    private val userSettingsRepository: UserSettingsRepository
) {

    operator fun invoke(query: String): Flow<List<Memo>> = combine(
        memoRepository.observeMemos(),
        userSettingsRepository.observeMemoSortOrder()
    ) { memos, sortOrder ->
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            emptyList()
        } else {
            memos
                .filter { memo ->
                    memo.title.value.contains(trimmed, ignoreCase = true) ||
                        memo.body.value.contains(trimmed, ignoreCase = true)
                }
                .sortedBy(sortOrder)
        }
    }
}
