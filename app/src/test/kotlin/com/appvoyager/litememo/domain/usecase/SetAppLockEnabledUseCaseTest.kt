package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.repository.FakeUserSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SetAppLockEnabledUseCaseTest {

    @Test
    fun invokePersistsAppLockEnabled() = runTest {
        // Arrange
        val repository = FakeUserSettingsRepository()
        val useCase = SetAppLockEnabledUseCase(repository)

        // Act
        useCase(true)

        // Assert
        assertEquals(true, repository.observeAppLockEnabled().first())
    }
}
