package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.repository.MemoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTrashedMemosUseCase @Inject constructor(private val memoRepository: MemoRepository) {

    operator fun invoke(): Flow<List<Memo>> = memoRepository.observeTrashedMemos()

}
