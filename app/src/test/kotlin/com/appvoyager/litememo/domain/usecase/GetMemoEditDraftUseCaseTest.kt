package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.FakeMemoEditDraftRepository
import com.appvoyager.litememo.domain.model.MemoEditDraft
import com.appvoyager.litememo.domain.model.MemoEditDraftTarget
import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoTitle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GetMemoEditDraftUseCaseTest {

    @Test
    fun invokeReturnsDraftWhenDraftExists() = runTest {
        // Arrange
        val draft = MemoEditDraft(
            target = MemoEditDraftTarget.newMemo(null),
            title = MemoTitle("Title"),
            body = MemoBody("Body"),
            createdAt = null,
            tagIds = emptyList(),
            isFavorite = false
        )
        val repository = FakeMemoEditDraftRepository(listOf(draft))
        val useCase = GetMemoEditDraftUseCase(repository)

        // Act
        val result = useCase(draft.target)

        // Assert
        assertEquals(draft, result)
    }

    @Test
    fun invokeReturnsNullWhenDraftDoesNotExist() = runTest {
        // Arrange
        val repository = FakeMemoEditDraftRepository()
        val useCase = GetMemoEditDraftUseCase(repository)

        // Act
        val result = useCase(MemoEditDraftTarget.newMemo(null))

        // Assert
        assertNull(result)
    }
}
