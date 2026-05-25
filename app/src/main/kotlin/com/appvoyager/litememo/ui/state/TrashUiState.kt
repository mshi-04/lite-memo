package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.Tag

data class TrashUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val hasActionError: Boolean = false,
    val memos: List<TrashedMemoUiModel> = emptyList(),
    val showPermanentDeleteDialog: TrashedMemoUiModel? = null
)

data class TrashedMemoUiModel(
    val id: String,
    val title: String,
    val body: String,
    val tags: List<TagUiModel>,
    val deletedAtMillis: Long
) {
    companion object {
        fun fromDomain(memos: List<Memo>, tags: List<Tag>): List<TrashedMemoUiModel> {
            val tagsById = tags.associateBy { it.id }
            return memos.mapNotNull { memo ->
                val deletedAt = memo.deletedAt ?: return@mapNotNull null
                TrashedMemoUiModel(
                    id = memo.id.value,
                    title = memo.title.value,
                    body = memo.body.value,
                    tags = memo.tagIds.mapNotNull { id ->
                        tagsById[id]?.let { TagUiModel.fromDomain(it) }
                    },
                    deletedAtMillis = deletedAt.value
                )
            }
        }
    }
}
