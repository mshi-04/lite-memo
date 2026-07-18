package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.MemoSummary
import com.appvoyager.litememo.domain.repository.MemoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * ウィジェット向けに「最近のメモ」を上限件数だけ観測する。
 * 並びは お気に入り優先 → 更新新しい順 で、DAO 側で LIMIT するため全件ロードしない。
 */
class ObserveRecentMemosUseCase @Inject constructor(private val memoRepository: MemoRepository) {

    operator fun invoke(limit: Int): Flow<List<MemoSummary>> =
        memoRepository.observeRecentActiveMemos(limit)

}
