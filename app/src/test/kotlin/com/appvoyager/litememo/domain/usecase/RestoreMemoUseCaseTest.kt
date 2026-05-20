package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.memoFixture
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RestoreMemoUseCaseTest {

    @Test
    fun invokeSavesMemoToRepository() = runTest {
        // Arrange
        val memo = memoFixture(id = "memo-1")
        val repository = FakeMemoRepository()
        val useCase = RestoreMemoUseCase(repository)

        // Act
        useCase(memo)

        // Assert
        assertEquals(listOf(memo.id), repository.currentMemos().map { it.id })
    }

}
