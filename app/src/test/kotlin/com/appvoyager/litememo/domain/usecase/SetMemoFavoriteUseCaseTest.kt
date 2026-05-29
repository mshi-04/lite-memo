package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.MutableTimeProvider
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SetMemoFavoriteUseCaseTest {

    @Test
    fun invokeSavesMemoWithUpdatedFavoriteState() = runTest {
        // Arrange
        val memo = memoFixture(id = "memo-1", isFavorite = false)
        val repository = FakeMemoRepository(listOf(memo))
        val useCase = SetMemoFavoriteUseCase(
            memoRepository = repository,
            currentTimeProvider = MutableTimeProvider(TimestampMillis(2000L))
        )

        // Act
        useCase(MemoId("memo-1"), true)

        // Assert
        assertEquals(true, repository.currentMemos().single().isFavorite)
    }

    @Test
    fun invokeUpdatesUpdatedAtWhenFavoriteStateChanges() = runTest {
        // Arrange
        val memo = memoFixture(id = "memo-1", updatedAt = 1000L)
        val repository = FakeMemoRepository(listOf(memo))
        val useCase = SetMemoFavoriteUseCase(
            memoRepository = repository,
            currentTimeProvider = MutableTimeProvider(TimestampMillis(2000L))
        )

        // Act
        val updatedMemo = useCase(MemoId("memo-1"), true)

        // Assert
        assertEquals(TimestampMillis(2000L), updatedMemo.updatedAt)
    }

    @Test
    fun invokeKeepsUpdatedAtWhenFavoriteStateDoesNotChange() = runTest {
        // Arrange
        val memo = memoFixture(id = "memo-1", updatedAt = 1000L, isFavorite = true)
        val repository = FakeMemoRepository(listOf(memo))
        val useCase = SetMemoFavoriteUseCase(
            memoRepository = repository,
            currentTimeProvider = MutableTimeProvider(TimestampMillis(2000L))
        )

        // Act
        val updatedMemo = useCase(MemoId("memo-1"), true)

        // Assert
        assertEquals(TimestampMillis(1000L), updatedMemo.updatedAt)
    }

    @Test
    fun invokeThrowsWhenMemoDoesNotExist() = runTest {
        // Arrange
        val useCase = SetMemoFavoriteUseCase(
            memoRepository = FakeMemoRepository(),
            currentTimeProvider = MutableTimeProvider()
        )

        // Act & Assert
        val error = runCatching { useCase(MemoId("missing"), true) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

}
