package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.sortedBy
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.repository.UserSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

class SearchMemosUseCase @Inject constructor(
    private val memoRepository: MemoRepository,
    private val userSettingsRepository: UserSettingsRepository
) {

    operator fun invoke(query: String): Flow<List<Memo>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return flowOf(emptyList())
        }

        return combine(
            memoRepository.observeMemosBySearchQuery(trimmed),
            userSettingsRepository.observeMemoSortOrder()
        ) { memos, sortOrder ->
            memos.sortedBy(sortOrder)
        }
    }
}
