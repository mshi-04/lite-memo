package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.memoFixture
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetMemoUseCaseTest {

    @Test
    fun invokeReturnsMemoById() = runBlocking {
        // Arrange
        val memo = memoFixture(id = "memo-1")
        val repository = FakeMemoRepository(listOf(memo))

        // Act
        val result = GetMemoUseCase(repository)(memo.id)

        // Assert
        assertEquals(memo, result)
    }

}
