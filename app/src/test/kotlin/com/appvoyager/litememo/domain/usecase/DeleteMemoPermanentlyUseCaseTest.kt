package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.memoFixture
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class DeleteMemoPermanentlyUseCaseTest {

    @Test
    fun invokeDeletesMemoPermanentlyById() = runTest {
        // Arrange
        val memo = memoFixture(deletedAt = 2_000L)
        val repository = FakeMemoRepository(listOf(memo))
        val useCase = DeleteMemoPermanentlyUseCase(repository)

        // Act
        useCase(memo.id)

        // Assert
        assertEquals(listOf(memo.id), repository.permanentlyDeletedIds)
    }

    @Test
    fun invokeThrowsWhenMemoIsActive() {
        // Arrange
        val memo = memoFixture()
        val repository = FakeMemoRepository(listOf(memo))
        val useCase = DeleteMemoPermanentlyUseCase(repository)

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase(memo.id) }
        }
    }
}
