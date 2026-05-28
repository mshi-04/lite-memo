package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.litememo.domain.model.ThemeMode
import com.appvoyager.litememo.domain.usecase.ObserveAppLockEnabledUseCase
import com.appvoyager.litememo.domain.usecase.ObserveThemeModeUseCase
import com.appvoyager.litememo.ui.auth.AppLockAuthenticationResult
import com.appvoyager.litememo.ui.state.AppLockMessage
import com.appvoyager.litememo.ui.state.AppLockStatus
import com.appvoyager.litememo.ui.state.AppLockUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val observeThemeModeUseCase: ObserveThemeModeUseCase,
    private val observeAppLockEnabledUseCase: ObserveAppLockEnabledUseCase
) : ViewModel() {

    val themeMode: Flow<ThemeMode> = observeThemeModeUseCase()

    private val _appLockUiState = MutableStateFlow(AppLockUiState())
    val appLockUiState: StateFlow<AppLockUiState> = _appLockUiState

    private val _authenticationRequestEvent = Channel<Unit>(Channel.BUFFERED)
    val authenticationRequestEvent = _authenticationRequestEvent.receiveAsFlow()

    private var appLockEnabled: Boolean? = null

    init {
        observeAppLockEnabled()
    }

    fun onAppStarted() {
        if (appLockEnabled == true && _appLockUiState.value.status == AppLockStatus.LOCKED) {
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
    }

    private fun observeAppLockEnabled() {
        viewModelScope.launch {
            observeAppLockEnabledUseCase().collect { enabled ->
                val previous = appLockEnabled
                appLockEnabled = enabled

                when {
                    !enabled -> {
                        _appLockUiState.value = AppLockUiState(status = AppLockStatus.UNLOCKED)
                    }

                    previous == null -> {
                        requestUnlock()
                    }

                    previous == false -> {
                        _appLockUiState.update { state ->
                            state.copy(status = AppLockStatus.UNLOCKED, message = null)
                        }
                    }
                }
            }
        }
    }
}
