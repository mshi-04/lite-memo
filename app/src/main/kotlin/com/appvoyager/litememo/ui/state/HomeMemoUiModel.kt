package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.TagColor

data class HomeMemoUiModel(
    val id: MemoId,
    val title: String,
    val body: String,
    val tagName: String?,
    val tagColor: TagColor?,
    val updatedAtMillis: Long,
    val isImportant: Boolean
)
