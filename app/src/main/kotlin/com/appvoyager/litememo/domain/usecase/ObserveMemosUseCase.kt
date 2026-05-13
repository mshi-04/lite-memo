package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.repository.MemoRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveMemosUseCase @Inject constructor(private val memoRepository: MemoRepository) {

    operator fun invoke(): Flow<List<Memo>> = memoRepository.observeMemos()
        .map { memos -> memos.sortedByDescending { it.updatedAt.value } }

}
