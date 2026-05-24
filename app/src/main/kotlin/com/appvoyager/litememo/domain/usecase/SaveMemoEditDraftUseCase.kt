package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.MemoEditDraft
import com.appvoyager.litememo.domain.repository.MemoEditDraftRepository
import javax.inject.Inject

class SaveMemoEditDraftUseCase @Inject constructor(
    private val repository: MemoEditDraftRepository
) {

    suspend operator fun invoke(draft: MemoEditDraft) {
        if (draft.title.isBlank() && draft.body.isBlank()) {
            repository.clearDraft(draft.target)
            return
        }

        repository.saveDraft(draft)
    }
}
