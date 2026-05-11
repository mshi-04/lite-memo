package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.memoFixture
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeleteMemoUseCaseTest {

    @Test
    fun invokeDeletesMemoById() = runBlocking {
        // Arrange
        val memo = memoFixture(id = "memo-1")
        val repository = FakeMemoRepository(listOf(memo))

        // Act
        DeleteMemoUseCase(repository)(memo.id)

        // Assert
        assertEquals(listOf(memo.id), repository.deletedIds)
    }

}
