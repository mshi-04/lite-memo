package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.ThemeMode
import com.appvoyager.litememo.domain.repository.FakeUserSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SetThemeModeUseCaseTest {

    @Test
    fun invokePersistsThemeMode() = runTest {
        // Arrange
        val repository = FakeUserSettingsRepository()
        val useCase = SetThemeModeUseCase(repository)

        // Act
        useCase(ThemeMode.LIGHT)

        // Assert
        assertEquals(ThemeMode.LIGHT, repository.observeThemeMode().first())
    }
}
