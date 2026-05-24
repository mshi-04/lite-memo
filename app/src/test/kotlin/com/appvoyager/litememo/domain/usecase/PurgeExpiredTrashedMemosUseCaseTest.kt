package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.MutableTimeProvider
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PurgeExpiredTrashedMemosUseCaseTest {

    @Test
    fun invokeDeletesTrashedMemosDeletedBeforeThirtyDaysAgo() = runTest {
        // Arrange
        val repository = FakeMemoRepository()
        val useCase = PurgeExpiredTrashedMemosUseCase(
            memoRepository = repository,
            currentTimeProvider = MutableTimeProvider(TimestampMillis(THIRTY_DAYS + 1_000L))
        )

        // Act
        useCase()

        // Assert
        assertEquals(listOf(TimestampMillis(1_000L)), repository.purgeCutoffs)
    }

    @Test
    fun invokeDeletesTrashedMemosDeletedAtThirtyDaysAgo() = runTest {
        // Arrange
        val memo = memoFixture(id = "memo-1", deletedAt = 1_000L)
        val repository = FakeMemoRepository(listOf(memo))
        val useCase = PurgeExpiredTrashedMemosUseCase(
            memoRepository = repository,
            currentTimeProvider = MutableTimeProvider(TimestampMillis(THIRTY_DAYS + 1_000L))
        )

        // Act
        useCase()

        // Assert
        assertEquals(0, repository.currentMemos().size)
    }

    @Test
    fun invokeKeepsTrashedMemosDeletedAfterThirtyDaysAgo() = runTest {
        // Arrange
        val memo = memoFixture(id = "memo-1", deletedAt = 1_001L)
        val repository = FakeMemoRepository(listOf(memo))
        val useCase = PurgeExpiredTrashedMemosUseCase(
            memoRepository = repository,
            currentTimeProvider = MutableTimeProvider(TimestampMillis(THIRTY_DAYS + 1_000L))
        )

        // Act
        useCase()

        // Assert
        assertEquals(listOf(memo), repository.currentMemos())
    }

    private companion object {
        const val THIRTY_DAYS = 30L * 24L * 60L * 60L * 1_000L
    }
}
