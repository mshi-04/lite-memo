package com.appvoyager.litememo.domain.model

import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis

data class MemoEditDraft(
    val target: MemoEditDraftTarget,
    val title: String,
    val body: String,
    val createdAt: TimestampMillis? = null,
    val tagIds: List<TagId> = emptyList(),
    val isFavorite: Boolean = false
)
