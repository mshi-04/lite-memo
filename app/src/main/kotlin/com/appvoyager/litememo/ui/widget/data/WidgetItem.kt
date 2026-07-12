package com.appvoyager.litememo.ui.widget.data

import com.appvoyager.litememo.domain.model.value.MemoId

data class WidgetItem(
    val id: MemoId,
    val title: String,
    val snippet: String,
    val isFavorite: Boolean
)
