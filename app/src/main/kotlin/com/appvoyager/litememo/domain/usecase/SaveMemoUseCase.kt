package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.SaveMemoCommand
import com.appvoyager.litememo.domain.model.updatedAtFrom
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.provider.CurrentTimeProvider
import com.appvoyager.litememo.domain.provider.MemoIdProvider
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.repository.TagRepository
import javax.inject.Inject

class SaveMemoUseCase @Inject constructor(
    private val memoRepository: MemoRepository,
    private val tagRepository: TagRepository,
    private val memoIdProvider: MemoIdProvider,
    private val currentTimeProvider: CurrentTimeProvider
) {

    suspend operator fun invoke(command: SaveMemoCommand): Memo {
        // タグやお気に入りだけではメモを保存しない。本文またはタイトルが必須。
        require(command.title.value.isNotBlank() || command.body.value.isNotBlank()) {
            "Memo title or body must not be blank."
        }
        val now = currentTimeProvider.now()
        val existingMemo = command.id?.let { id -> memoRepository.getActiveMemo(id) }
        val tagIds = command.tagIds.distinct()
        validateTagIds(tagIds)
        val createdAt = existingMemo?.createdAt ?: command.createdAt ?: now
        val updatedAt = existingMemo?.updatedAtFrom(now)
            ?: TimestampMillis(maxOf(now.value, createdAt.value))
        val memo = Memo(
            id = existingMemo?.id ?: command.id ?: memoIdProvider.newMemoId(),
            title = command.title,
            body = command.body,
            createdAt = createdAt,
            updatedAt = updatedAt,
            tagIds = tagIds,
            isFavorite = command.isFavorite
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
