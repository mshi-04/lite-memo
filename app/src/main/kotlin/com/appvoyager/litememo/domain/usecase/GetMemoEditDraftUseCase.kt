package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.MemoEditDraft
import com.appvoyager.litememo.domain.model.MemoEditDraftTarget
import com.appvoyager.litememo.domain.repository.MemoEditDraftRepository
import javax.inject.Inject

class GetMemoEditDraftUseCase @Inject constructor(private val repository: MemoEditDraftRepository) {

    suspend operator fun invoke(target: MemoEditDraftTarget): MemoEditDraft? =
        repository.getDraft(target)
}
