package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.repository.FakeUserSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ObserveMemosUseCaseTest {

    @Test
    fun invokeReturnsMemosSortedByUpdatedAtDescending() = runBlocking {
        // Arrange
        val older = memoFixture(id = "older", updatedAt = 1000L)
        val newer = memoFixture(id = "newer", updatedAt = 2000L)
        val repository = FakeMemoRepository(listOf(older, newer))

        // Act
        val memos = ObserveMemosUseCase(repository, FakeUserSettingsRepository())().first()

        // Assert
        assertEquals(listOf(newer.id, older.id), memos.map { it.id })
    }

}
