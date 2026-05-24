package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.repository.MemoRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveTrashedMemosUseCase @Inject constructor(private val memoRepository: MemoRepository) {

    operator fun invoke(): Flow<List<Memo>> = memoRepository.observeTrashedMemos()

}
