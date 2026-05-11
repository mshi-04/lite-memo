package com.appvoyager.litememo.domain.model

import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.TagId

data class SaveMemoCommand(
    val id: MemoId? = null,
    val title: String,
    val body: String,
    val tagIds: List<TagId> = emptyList(),
    val isImportant: Boolean = false
)
