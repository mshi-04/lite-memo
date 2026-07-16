package com.appvoyager.litememo.ui.model

import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.ui.model.TagUiModel

data class TrashedMemoUiModel(
    val id: MemoId,
    val title: String,
    val body: String,
    val tags: List<TagUiModel>,
    val deletedAt: TimestampMillis
)
