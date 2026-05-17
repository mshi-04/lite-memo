package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.Tag

data class MemoUiModel(
    val id: String,
    val title: String,
    val body: String,
    val tagName: String?,
    val tagColorArgb: Long?,
    val updatedAtMillis: Long,
    val isImportant: Boolean
) {
    companion object {
        fun fromDomain(memos: List<Memo>, tags: List<Tag>): List<MemoUiModel> {
            val tagsById = tags.associateBy { it.id }
            return memos.map { memo ->
                val tag = memo.tagIds.firstNotNullOfOrNull { id -> tagsById[id] }
                MemoUiModel(
                    id = memo.id.value,
                    title = memo.title.value,
                    body = memo.body.value,
                    tagName = tag?.name?.value,
                    tagColorArgb = tag?.color?.argb,
                    updatedAtMillis = memo.updatedAt.value,
                    isImportant = memo.isImportant
                )
            }
        }
    }
}
