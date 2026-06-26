package com.appvoyager.litememo.ui.viewmodel

import app.cash.turbine.test
import com.appvoyager.litememo.domain.repository.FakeUserSettingsRepository
import com.appvoyager.litememo.domain.usecase.ObserveAppLockEnabledUseCase
import com.appvoyager.litememo.domain.usecase.ObserveThemeModeUseCase
import com.appvoyager.litememo.ui.auth.AppLockAuthenticationResult
import com.appvoyager.litememo.ui.state.AppLockMessage
import com.appvoyager.litememo.ui.state.AppLockStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private lateinit var dispatcher: TestDispatcher

    @BeforeEach
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun appLockDisabledShowsAppContent() = runTest(dispatcher) {
        // Arrange
        val viewModel = mainViewModel(FakeUserSettingsRepository())

        // Act
        advanceUntilIdle()

        // Assert
        assertEquals(AppLockStatus.UNLOCKED, viewModel.appLockUiState.value.status)
    }

    @Test
    fun appLockEnabledRequestsAuthentication() = runTest(dispatcher) {
        // Arrange
        val repository = FakeUserSettingsRepository()
        repository.setAppLockEnabled(true)
        val viewModel = mainViewModel(repository)

        // Act
        advanceUntilIdle()

        // Assert
        assertEquals(AppLockStatus.AUTHENTICATING, viewModel.appLockUiState.value.status)
    }

    @Test
    fun flowAppLockEnabledEmitsAuthenticationRequestEvent() = runTest(dispatcher) {
        // Arrange
        val repository = FakeUserSettingsRepository()
        repository.setAppLockEnabled(true)
        val viewModel = mainViewModel(repository)

        // Act
        advanceUntilIdle()

        // Assert
        viewModel.authenticationRequestEvent.test {
            assertEquals(Unit, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun flowRequestUnlockDoesNotEmitDuplicateEventWhileAuthenticating() = runTest(dispatcher) {
        // Arrange
        val repository = FakeUserSettingsRepository()
        repository.setAppLockEnabled(true)
        val viewModel = mainViewModel(repository)
        advanceUntilIdle()

        // Act & Assert
        viewModel.authenticationRequestEvent.test {
            assertEquals(Unit, awaitItem())
            viewModel.requestUnlock()
            advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun authenticationSuccessUnlocksApp() = runTest(dispatcher) {
        // Arrange
        val repository = FakeUserSettingsRepository()
        repository.setAppLockEnabled(true)
        val viewModel = mainViewModel(repository)
        advanceUntilIdle()

        // Act
        viewModel.onAuthenticationResult(AppLockAuthenticationResult.SUCCEEDED)

        // Assert
        assertEquals(AppLockStatus.UNLOCKED, viewModel.appLockUiState.value.status)
    }

    @Test
    fun appStartRetriesAuthenticationWhenUnavailable() = runTest(dispatcher) {
        // Arrange
        val repository = FakeUserSettingsRepository()
        repository.setAppLockEnabled(true)
        val viewModel = mainViewModel(repository)
        advanceUntilIdle()
        viewModel.onAuthenticationResult(AppLockAuthenticationResult.NO_DEVICE_CREDENTIAL)

        // Act
        viewModel.onAppStarted()

        // Assert
        assertEquals(AppLockStatus.AUTHENTICATING, viewModel.appLockUiState.value.status)
    }

    @Test
    fun authenticationFailureLocksApp() = runTest(dispatcher) {
        // Arrange
        val repository = FakeUserSettingsRepository()
        repository.setAppLockEnabled(true)
        val viewModel = mainViewModel(repository)
        advanceUntilIdle()

        // Act
        viewModel.onAuthenticationResult(AppLockAuthenticationResult.FAILED)

        // Assert
        assertEquals(AppLockStatus.LOCKED, viewModel.appLockUiState.value.status)
    }

    @Test
    fun authenticationCanceledLocksApp() = runTest(dispatcher) {
        // Arrange
        val repository = FakeUserSettingsRepository()
        repository.setAppLockEnabled(true)
        val viewModel = mainViewModel(repository)
        advanceUntilIdle()

        // Act
        viewModel.onAuthenticationResult(AppLockAuthenticationResult.CANCELED)

        // Assert
        assertEquals(AppLockStatus.LOCKED, viewModel.appLockUiState.value.status)
    }

    @Test
    fun errorNoDeviceCredentialSetsUnavailableStatusWithNoCredentialMessage() =
        runTest(dispatcher) {
            // Arrange
            val repository = FakeUserSettingsRepository()
            repository.setAppLockEnabled(true)
            val viewModel = mainViewModel(repository)
            advanceUntilIdle()

            // Act
            viewModel.onAuthenticationResult(AppLockAuthenticationResult.NO_DEVICE_CREDENTIAL)

            // Assert
            val state = viewModel.appLockUiState.value
            assertEquals(
                AppLockStatus.UNAVAILABLE to AppLockMessage.NO_DEVICE_CREDENTIAL,
                state.status to state.message
            )
        }

    @Test
    fun errorAuthenticationUnavailableSetsUnavailableStatusWithUnavailableMessage() =
        runTest(dispatcher) {
            // Arrange
            val repository = FakeUserSettingsRepository()
            repository.setAppLockEnabled(true)
            val viewModel = mainViewModel(repository)
            advanceUntilIdle()

            // Act
            viewModel.onAuthenticationResult(AppLockAuthenticationResult.UNAVAILABLE)

            // Assert
            val state = viewModel.appLockUiState.value
            assertEquals(
                AppLockStatus.UNAVAILABLE to AppLockMessage.AUTHENTICATION_UNAVAILABLE,
                state.status to state.message
            )
        }

    @Test
    fun secureScreenReflectsAppLockEnabled() = runTest(dispatcher) {
        // Arrange
        val repository = FakeUserSettingsRepository()
        repository.setAppLockEnabled(true)
        val viewModel = mainViewModel(repository)

        // Act
        advanceUntilIdle()

        // Assert
        assertEquals(true, viewModel.secureScreenEnabled.value)
    }

    @Test
    fun appStopLocksAppWhenAppLockIsEnabled() = runTest(dispatcher) {
        // Arrange
        val repository = FakeUserSettingsRepository()
        repository.setAppLockEnabled(true)
        val viewModel = mainViewModel(repository)
        advanceUntilIdle()
        viewModel.onAuthenticationResult(AppLockAuthenticationResult.SUCCEEDED)

        // Act
        viewModel.onAppStopped()

        // Assert
        assertEquals(AppLockStatus.LOCKED, viewModel.appLockUiState.value.status)
    }

    @Test
    fun appStopKeepsAppUnlockedWhenAppLockIsDisabled() = runTest(dispatcher) {
        // Arrange
        val viewModel = mainViewModel(FakeUserSettingsRepository())
        advanceUntilIdle()

        // Act
        viewModel.onAppStopped()

        // Assert
        assertEquals(AppLockStatus.UNLOCKED, viewModel.appLockUiState.value.status)
    }

    @Test
    fun stateTransitionDisablingAppLockUnlocksAppAndClearsMessage() = runTest(dispatcher) {
        // Arrange
        val repository = FakeUserSettingsRepository()
        repository.setAppLockEnabled(true)
        val viewModel = mainViewModel(repository)
        advanceUntilIdle()
        viewModel.onAuthenticationResult(AppLockAuthenticationResult.FAILED)

        // Act
        repository.setAppLockEnabled(false)
        advanceUntilIdle()

        // Assert
        val state = viewModel.appLockUiState.value
        assertEquals(AppLockStatus.UNLOCKED to null, state.status to state.message)
    }

    @Test
    fun stateTransitionAppStopKeepsAuthenticatingStateWhenAuthenticationIsInProgress() =
        runTest(dispatcher) {
            // Arrange
            val repository = FakeUserSettingsRepository()
            repository.setAppLockEnabled(true)
            val viewModel = mainViewModel(repository)
            advanceUntilIdle()

            // Act
            viewModel.onAppStopped()

            // Assert
            assertEquals(AppLockStatus.AUTHENTICATING, viewModel.appLockUiState.value.status)
        }

    private fun mainViewModel(repository: FakeUserSettingsRepository) = MainViewModel(
        observeThemeModeUseCase = ObserveThemeModeUseCase(repository),
        observeAppLockEnabledUseCase = ObserveAppLockEnabledUseCase(repository)
    )
}
