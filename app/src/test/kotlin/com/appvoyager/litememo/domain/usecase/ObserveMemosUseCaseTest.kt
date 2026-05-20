package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.MemoSortOrder
import com.appvoyager.litememo.domain.repository.FakeUserSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ObserveMemosUseCaseTest {

    @Test
    fun invokeReturnsMemosSortedByUpdatedAtDescending() = runTest {
        // Arrange
        val older = memoFixture(id = "older", updatedAt = 1000L)
        val newer = memoFixture(id = "newer", updatedAt = 2000L)
        val repository = FakeMemoRepository(listOf(older, newer))

        // Act
        val memos = ObserveMemosUseCase(repository, FakeUserSettingsRepository())().first()

        // Assert
        assertEquals(listOf(newer.id, older.id), memos.map { it.id })
    }

    @Test
    fun invokeReturnsImportantMemosBeforeNormalMemos() = runTest {
        // Arrange
        val normal = memoFixture(id = "normal", updatedAt = 2000L)
        val important = memoFixture(id = "important", updatedAt = 1000L, isImportant = true)
        val repository = FakeMemoRepository(listOf(normal, important))

        // Act
        val memos = ObserveMemosUseCase(repository, FakeUserSettingsRepository())().first()

        // Assert
        assertEquals(listOf(important.id, normal.id), memos.map { it.id })
    }

    @Test
    fun invokeReturnsImportantMemosBeforeNormalMemosWhenSortedByCreatedAt() = runTest {
        // Arrange
        val normal = memoFixture(id = "normal", createdAt = 2000L)
        val important = memoFixture(id = "important", createdAt = 1000L, isImportant = true)
        val repository = FakeMemoRepository(listOf(normal, important))
        val settingsRepository = FakeUserSettingsRepository()
        settingsRepository.setMemoSortOrder(MemoSortOrder.CREATED_NEWEST)

        // Act
        val memos = ObserveMemosUseCase(repository, settingsRepository)().first()

        // Assert
        assertEquals(listOf(important.id, normal.id), memos.map { it.id })
    }

}
