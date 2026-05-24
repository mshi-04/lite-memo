package com.appvoyager.litememo.domain.model

import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.MemoTitle
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis

data class SaveMemoCommand(
    val id: MemoId? = null,
    val title: MemoTitle,
    val body: MemoBody,
    val createdAt: TimestampMillis? = null,
    val tagIds: List<TagId> = emptyList(),
    val isFavorite: Boolean = false
)
