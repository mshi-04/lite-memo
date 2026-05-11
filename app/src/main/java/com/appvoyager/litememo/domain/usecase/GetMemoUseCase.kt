package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.repository.MemoRepository

class GetMemoUseCase(private val memoRepository: MemoRepository) {

    suspend operator fun invoke(id: MemoId): Memo? = memoRepository.getMemo(id)
}
