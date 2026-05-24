package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.FakeMemoEditDraftRepository
import com.appvoyager.litememo.domain.model.MemoEditDraft
import com.appvoyager.litememo.domain.model.MemoEditDraftTarget
import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoTitle
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SaveMemoEditDraftUseCaseTest {

    @Test
    fun invokeSavesDraftWhenTitleIsNotBlank() = runTest {
        // Arrange
        val repository = FakeMemoEditDraftRepository()
        val useCase = SaveMemoEditDraftUseCase(repository)
        val draft = memoEditDraft(title = "Title")

        // Act
        useCase(draft)

        // Assert
        assertEquals(listOf(draft), repository.savedDrafts)
    }

    @Test
    fun invokeClearsDraftWhenTitleAndBodyAreBlank() = runTest {
        // Arrange
        val repository = FakeMemoEditDraftRepository()
        val useCase = SaveMemoEditDraftUseCase(repository)
        val target = MemoEditDraftTarget.newMemo(null)

        // Act
        useCase(memoEditDraft(target = target, title = " ", body = " "))

        // Assert
        assertEquals(listOf(target), repository.clearedTargets)
    }

    @Test
    fun invokePreservesDraftFieldsWhenSaving() = runTest {
        // Arrange
        val repository = FakeMemoEditDraftRepository()
        val useCase = SaveMemoEditDraftUseCase(repository)
        val draft = memoEditDraft(
            title = "Title",
            body = "Body",
            createdAt = TimestampMillis(1000L),
            tagIds = listOf(TagId("tag-1")),
            isFavorite = true
        )

        // Act
        useCase(draft)

        // Assert
        assertEquals(draft, repository.currentDrafts().single())
    }

    private fun memoEditDraft(
        target: MemoEditDraftTarget = MemoEditDraftTarget.newMemo(null),
        title: String = "",
        body: String = "",
        createdAt: TimestampMillis? = null,
        tagIds: List<TagId> = emptyList(),
        isFavorite: Boolean = false
    ) = MemoEditDraft(
        target = target,
        title = MemoTitle(title),
        body = MemoBody(body),
        createdAt = createdAt,
        tagIds = tagIds,
        isFavorite = isFavorite
    )
}
