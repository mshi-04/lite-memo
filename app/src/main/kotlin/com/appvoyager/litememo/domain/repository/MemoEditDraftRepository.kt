package com.appvoyager.litememo.domain.repository

import com.appvoyager.litememo.domain.model.MemoEditDraft
import com.appvoyager.litememo.domain.model.MemoEditDraftTarget

interface MemoEditDraftRepository {

    suspend fun getDraft(target: MemoEditDraftTarget): MemoEditDraft?

    suspend fun saveDraft(draft: MemoEditDraft)

    suspend fun clearDraft(target: MemoEditDraftTarget)
}
