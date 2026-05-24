package com.appvoyager.litememo.domain.model

import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.TimestampMillis

@JvmInline
value class MemoEditDraftTarget private constructor(val value: String) {

    companion object {
        fun existingMemo(id: MemoId): MemoEditDraftTarget = MemoEditDraftTarget("memo_${id.value}")

        fun newMemo(createdAt: TimestampMillis?): MemoEditDraftTarget =
            MemoEditDraftTarget(createdAt?.let { "new_createdAt_${it.value}" } ?: "new_default")
    }
}
