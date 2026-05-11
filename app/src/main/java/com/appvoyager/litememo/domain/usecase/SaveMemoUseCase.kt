package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.SaveMemoCommand
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.provider.CurrentTimeProvider
import com.appvoyager.litememo.domain.provider.MemoIdProvider
import com.appvoyager.litememo.domain.repository.MemoRepository

class SaveMemoUseCase(
    private val memoRepository: MemoRepository,
    private val memoIdProvider: MemoIdProvider,
    private val currentTimeProvider: CurrentTimeProvider
) {

    suspend operator fun invoke(command: SaveMemoCommand): Result<Memo> {
        val now = currentTimeProvider.now()
        val existingMemo = command.id?.let { id ->
            memoRepository.getMemo(id)
                ?: return Result.failure(SaveMemoError.MemoNotFound(id))
        }

        val tagIds = command.tagIds.distinct()
        val memo = Memo(
            id = existingMemo?.id ?: memoIdProvider.newMemoId(),
            title = command.title,
            body = command.body,
            createdAt = existingMemo?.createdAt ?: now,
            updatedAt = now,
            tagIds = tagIds,
            isImportant = command.isImportant
        )

        return memoRepository.saveMemoWithTagCheck(memo)
            .fold(
                onSuccess = { Result.success(memo) },
                onFailure = { Result.failure(it) }
            )
    }
}

sealed class SaveMemoError(message: String) : RuntimeException(message) {

    data class MemoNotFound(val id: MemoId) : SaveMemoError("Memo not found: ${id.value}")

    data class TagsNotFound(val ids: List<TagId>) :
        SaveMemoError("Memo references tags that do not exist: ${ids.joinToString { it.value }}.")
}
