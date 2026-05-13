package com.appvoyager.litememo.ui.state

data class HomeMemoUiModel(
    val id: String,
    val title: String,
    val body: String,
    val tagName: String?,
    val tagColorArgb: Long?,
    val updatedAtMillis: Long,
    val isImportant: Boolean
)
