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

class SetMemoImportantUseCaseTest {

    @Test
    fun invokeSavesMemoWithUpdatedImportantState() = runTest {
        // Arrange
        val memo = memoFixture(id = "memo-1", isImportant = false)
        val repository = FakeMemoRepository(listOf(memo))
        val useCase = SetMemoImportantUseCase(
            memoRepository = repository,
            currentTimeProvider = MutableTimeProvider(TimestampMillis(2000L))
        )

        // Act
        useCase(MemoId("memo-1"), true)

        // Assert
        assertEquals(true, repository.currentMemos().single().isImportant)
    }

    @Test
    fun invokeUpdatesUpdatedAtWhenImportantStateChanges() = runTest {
        // Arrange
        val memo = memoFixture(id = "memo-1", updatedAt = 1000L)
        val repository = FakeMemoRepository(listOf(memo))
        val useCase = SetMemoImportantUseCase(
            memoRepository = repository,
            currentTimeProvider = MutableTimeProvider(TimestampMillis(2000L))
        )

        // Act
        val updatedMemo = useCase(MemoId("memo-1"), true)

        // Assert
        assertEquals(TimestampMillis(2000L), updatedMemo.updatedAt)
    }

    @Test
    fun invokeKeepsUpdatedAtWhenImportantStateDoesNotChange() = runTest {
        // Arrange
        val memo = memoFixture(id = "memo-1", updatedAt = 1000L, isImportant = true)
        val repository = FakeMemoRepository(listOf(memo))
        val useCase = SetMemoImportantUseCase(
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
        val useCase = SetMemoImportantUseCase(
            memoRepository = FakeMemoRepository(),
            currentTimeProvider = MutableTimeProvider()
        )

        // Act & Assert
        val error = runCatching { useCase(MemoId("missing"), true) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

}
