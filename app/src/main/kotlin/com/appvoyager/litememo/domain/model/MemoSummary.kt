package com.appvoyager.litememo.domain.model

import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.MemoTitle

data class MemoSummary(
    val id: MemoId,
    val title: MemoTitle,
    val body: MemoBody,
    val isFavorite: Boolean
)
