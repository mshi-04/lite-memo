package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.MemoSummary
import com.appvoyager.litememo.domain.repository.MemoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRecentMemosUseCase @Inject constructor(private val memoRepository: MemoRepository) {

    operator fun invoke(limit: Int): Flow<List<MemoSummary>> =
        memoRepository.observeRecentActiveMemos(limit)

}
