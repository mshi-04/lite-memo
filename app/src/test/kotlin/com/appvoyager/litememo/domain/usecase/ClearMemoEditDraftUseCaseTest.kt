package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.FakeMemoEditDraftRepository
import com.appvoyager.litememo.domain.model.MemoEditDraftTarget
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ClearMemoEditDraftUseCaseTest {

    @Test
    fun invokeClearsDraftForTarget() = runTest {
        // Arrange
        val target = MemoEditDraftTarget.newMemo(null)
        val repository = FakeMemoEditDraftRepository()
        val useCase = ClearMemoEditDraftUseCase(repository)

        // Act
        useCase(target)

        // Assert
        assertEquals(listOf(target), repository.clearedTargets)
    }
}
