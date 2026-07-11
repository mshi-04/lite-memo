package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.litememo.domain.model.ThemeMode
import com.appvoyager.litememo.domain.usecase.CompleteTutorialUseCase
import com.appvoyager.litememo.domain.usecase.ObserveAppLockEnabledUseCase
import com.appvoyager.litememo.domain.usecase.ObserveThemeModeUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTutorialCompletedUseCase
import com.appvoyager.litememo.domain.usecase.PurgeExpiredTrashedMemosUseCase
import com.appvoyager.litememo.ui.auth.AppLockAuthenticationResult
import com.appvoyager.litememo.ui.navigation.WidgetNavRequest
import com.appvoyager.litememo.ui.state.AppLockMessage
import com.appvoyager.litememo.ui.state.AppLockStatus
import com.appvoyager.litememo.ui.state.AppLockUiState
import com.appvoyager.litememo.ui.state.TutorialStatus
import com.appvoyager.litememo.ui.state.TutorialUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val observeThemeModeUseCase: ObserveThemeModeUseCase,
    private val observeAppLockEnabledUseCase: ObserveAppLockEnabledUseCase,
    private val observeTutorialCompletedUseCase: ObserveTutorialCompletedUseCase,
    private val completeTutorialUseCase: CompleteTutorialUseCase,
    private val purgeExpiredTrashedMemosUseCase: PurgeExpiredTrashedMemosUseCase
) : ViewModel() {

    val themeMode: Flow<ThemeMode> = observeThemeModeUseCase()

    private val _appLockUiState = MutableStateFlow(AppLockUiState())
    val appLockUiState: StateFlow<AppLockUiState> = _appLockUiState.asStateFlow()

    private val _authenticationRequestEvent = Channel<Unit>(Channel.CONFLATED)
    val authenticationRequestEvent = _authenticationRequestEvent.receiveAsFlow()

    private val _secureScreenEnabled = MutableStateFlow(false)
    val secureScreenEnabled: StateFlow<Boolean> = _secureScreenEnabled.asStateFlow()

    private val _tutorialUiState = MutableStateFlow(TutorialUiState())
    val tutorialUiState: StateFlow<TutorialUiState> = _tutorialUiState.asStateFlow()

    private val _pendingWidgetNav = MutableStateFlow<WidgetNavRequest?>(null)
    val pendingWidgetNav: StateFlow<WidgetNavRequest?> = _pendingWidgetNav.asStateFlow()

    private var appLockEnabled: Boolean? = null
    private var expiredTrashedMemosPurged = false

    init {
        observeAppLockEnabled()
        observeTutorialCompleted()
    }

    fun onAppStarted() {
        if (appLockEnabled != true) return
        val status = _appLockUiState.value.status
        if (status == AppLockStatus.LOCKED || status == AppLockStatus.UNAVAILABLE) {
            requestUnlock()
        }
    }

    fun onAppStopped() {
        if (
            appLockEnabled == true &&
            _appLockUiState.value.status != AppLockStatus.AUTHENTICATING
        ) {
            _appLockUiState.value = AppLockUiState(status = AppLockStatus.LOCKED)
        }
    }

    fun requestUnlock() {
        if (appLockEnabled != true) return
        if (_appLockUiState.value.status == AppLockStatus.AUTHENTICATING) return

        _appLockUiState.value = AppLockUiState(status = AppLockStatus.AUTHENTICATING)
        _authenticationRequestEvent.trySend(Unit)
    }

    fun onAuthenticationResult(result: AppLockAuthenticationResult) {
        _appLockUiState.value = when (result) {
            AppLockAuthenticationResult.SUCCEEDED -> AppLockUiState(
                status = AppLockStatus.UNLOCKED
            )

            AppLockAuthenticationResult.FAILED -> AppLockUiState(
                status = AppLockStatus.LOCKED,
                message = AppLockMessage.AUTHENTICATION_FAILED
            )

            AppLockAuthenticationResult.CANCELED -> AppLockUiState(
                status = AppLockStatus.LOCKED,
                message = AppLockMessage.AUTHENTICATION_CANCELED
            )

            AppLockAuthenticationResult.NO_DEVICE_CREDENTIAL -> AppLockUiState(
                status = AppLockStatus.UNAVAILABLE,
                message = AppLockMessage.NO_DEVICE_CREDENTIAL
            )

            AppLockAuthenticationResult.UNAVAILABLE -> AppLockUiState(
                status = AppLockStatus.UNAVAILABLE,
                message = AppLockMessage.AUTHENTICATION_UNAVAILABLE
            )
        }
        if (result == AppLockAuthenticationResult.SUCCEEDED) {
            purgeExpiredTrashedMemosOnce()
        }
    }

    fun requestWidgetNav(request: WidgetNavRequest) {
        _pendingWidgetNav.value = request
    }

    fun consumeWidgetNav() {
        _pendingWidgetNav.value = null
    }

    fun completeTutorial() {
        _tutorialUiState.value = TutorialUiState(status = TutorialStatus.HIDDEN)
        viewModelScope.launch {
            try {
                completeTutorialUseCase()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
            }
        }
    }

    private fun observeAppLockEnabled() {
        viewModelScope.launch {
            observeAppLockEnabledUseCase().collect { enabled ->
                val previous = appLockEnabled
                appLockEnabled = enabled
                _secureScreenEnabled.value = enabled

                when {
                    !enabled -> {
                        _appLockUiState.value = AppLockUiState(status = AppLockStatus.UNLOCKED)
                        purgeExpiredTrashedMemosOnce()
                    }

                    previous == null -> {
                        requestUnlock()
                    }

                    previous == false -> {
                        _appLockUiState.update { state ->
                            state.copy(status = AppLockStatus.UNLOCKED, message = null)
                        }
                        purgeExpiredTrashedMemosOnce()
                    }
                }
            }
        }
    }

    private fun observeTutorialCompleted() {
        viewModelScope.launch {
            observeTutorialCompletedUseCase().collect { completed ->
                _tutorialUiState.update { state -> state.next(completed) }
            }
        }
    }

    private fun purgeExpiredTrashedMemosOnce() {
        if (expiredTrashedMemosPurged) return
        expiredTrashedMemosPurged = true
        viewModelScope.launch {
            try {
                purgeExpiredTrashedMemosUseCase()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
            }
        }
    }
}
