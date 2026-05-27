package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.ApplyMemoBulkActionCommand
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.MemoBulkAction
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.provider.CurrentTimeProvider
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.repository.TagRepository
import javax.inject.Inject

class ApplyMemoBulkActionUseCase @Inject constructor(
    private val memoRepository: MemoRepository,
    private val tagRepository: TagRepository,
    private val currentTimeProvider: CurrentTimeProvider
) {

    suspend operator fun invoke(command: ApplyMemoBulkActionCommand) {
        val memoIds = command.memoIds.distinct()
        if (memoIds.isEmpty()) return

        val memos = memoIds.map { id ->
            requireNotNull(memoRepository.getActiveMemo(id)) {
                "Memo not found: ${id.value}"
            }
        }
        val tagId = command.action.tagId
        if (tagId != null) {
            requireNotNull(tagRepository.getTag(tagId)) {
                "Tag not found: ${tagId.value}"
            }
        }

        applyAction(memos, command.action)
    }

    private suspend fun applyAction(memos: List<Memo>, action: MemoBulkAction) {
        when (action.type) {
            MemoBulkAction.Type.MoveToTrash -> moveToTrash(memos)

            MemoBulkAction.Type.SetFavorite -> setFavorite(
                memos = memos,
                isFavorite = requireNotNull(action.isFavorite)
            )

            MemoBulkAction.Type.AddTag -> addTag(
                memos = memos,
                tagId = requireNotNull(action.tagId)
            )

            MemoBulkAction.Type.RemoveTag -> removeTag(
                memos = memos,
                tagId = requireNotNull(action.tagId)
            )
        }
    }

    private suspend fun moveToTrash(memos: List<Memo>) {
        val now = currentTimeProvider.now()
        memos.forEach { memo ->
            memoRepository.moveMemoToTrash(
                id = memo.id,
                deletedAt = TimestampMillis(maxOf(now.value, memo.createdAt.value))
            )
        }
    }

    private suspend fun setFavorite(memos: List<Memo>, isFavorite: Boolean) {
        val now = currentTimeProvider.now()
        memos.forEach { memo ->
            if (memo.isFavorite != isFavorite) {
                memoRepository.saveMemo(
                    memo.copy(
                        updatedAt = now,
                        isFavorite = isFavorite
                    )
                )
            }
        }
    }

    private suspend fun addTag(memos: List<Memo>, tagId: TagId) {
        val now = currentTimeProvider.now()
        memos.forEach { memo ->
            if (tagId !in memo.tagIds) {
                memoRepository.saveMemo(
                    memo.copy(
                        updatedAt = now,
                        tagIds = memo.tagIds + tagId
                    )
                )
            }
        }
    }

    private suspend fun removeTag(memos: List<Memo>, tagId: TagId) {
        val now = currentTimeProvider.now()
        memos.forEach { memo ->
            if (tagId in memo.tagIds) {
                memoRepository.saveMemo(
                    memo.copy(
                        updatedAt = now,
                        tagIds = memo.tagIds.filterNot { it == tagId }
                    )
                )
            }
        }
    }
}
