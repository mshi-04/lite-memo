package com.appvoyager.litememo.domain.model

import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.MemoTitle
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis

data class Memo(
    val id: MemoId,
    val title: MemoTitle,
    val body: MemoBody,
    val createdAt: TimestampMillis,
    val updatedAt: TimestampMillis,
    val tagIds: List<TagId> = emptyList(),
    val isImportant: Boolean = false
) {

    init {
        require(updatedAt.value >= createdAt.value) {
            "Memo updatedAt must be greater than or equal to createdAt."
        }
        require(title.value.isNotBlank() || body.value.isNotBlank()) {
            "Memo title and body must not both be blank."
        }
    }

}
