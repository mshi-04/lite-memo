package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.MemoEditDraft
import com.appvoyager.litememo.domain.repository.MemoEditDraftRepository
import javax.inject.Inject

class SaveMemoEditDraftUseCase @Inject constructor(
    private val repository: MemoEditDraftRepository
) {

    suspend operator fun invoke(draft: MemoEditDraft) {
        val shouldClear =
            draft.title.isBlank() &&
                draft.body.isBlank() &&
                draft.tagIds.isEmpty() &&
                !draft.isFavorite
        if (shouldClear) {
            repository.clearDraft(draft.target)
            return
        }

        repository.saveDraft(draft)
    }
}
