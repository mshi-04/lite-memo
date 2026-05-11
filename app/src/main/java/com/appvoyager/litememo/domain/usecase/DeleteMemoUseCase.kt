package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.repository.MemoRepository

class DeleteMemoUseCase(private val memoRepository: MemoRepository) {

    suspend operator fun invoke(id: MemoId) = memoRepository.deleteMemo(id)
}
