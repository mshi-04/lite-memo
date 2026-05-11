package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.SaveMemoCommand
import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoTitle
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.provider.CurrentTimeProvider
import com.appvoyager.litememo.domain.provider.MemoIdProvider
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.repository.TagRepository

class SaveMemoUseCase(
    private val memoRepository: MemoRepository,
    private val tagRepository: TagRepository,
    private val memoIdProvider: MemoIdProvider,
    private val currentTimeProvider: CurrentTimeProvider
) {

    suspend operator fun invoke(command: SaveMemoCommand): Memo {
        val title = MemoTitle(command.title)
        val body = MemoBody(command.body)

        val now = currentTimeProvider.now()
        val existingMemo = command.id?.let { id ->
            memoRepository.getMemo(id)
                ?: throw NoSuchElementException("Memo not found: ${id.value}")
        }
        val tagIds = command.tagIds.distinct()
        validateTagIds(tagIds)
        val memo = Memo(
            id = existingMemo?.id ?: memoIdProvider.newMemoId(),
            title = title,
            body = body,
            createdAt = existingMemo?.createdAt ?: now,
            updatedAt = now,
            tagIds = tagIds,
            isImportant = command.isImportant
        )

        memoRepository.saveMemo(memo)
        return memo
    }

    private suspend fun validateTagIds(tagIds: List<TagId>) {
        if (tagIds.isEmpty()) return
        val existingIds = tagRepository.getTagsByIds(tagIds).map { it.id }.toSet()
        val missingIds = tagIds.filterNot { it in existingIds }
        require(missingIds.isEmpty()) {
            "Memo references tags that do not exist: ${missingIds.joinToString { it.value }}."
        }
    }

}
