package com.appvoyager.litememo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.appvoyager.litememo.domain.model.ThemeMode
import com.appvoyager.litememo.ui.auth.AppLockAuthenticator
import com.appvoyager.litememo.ui.navigation.LiteMemoApp
import com.appvoyager.litememo.ui.screen.AppLockScreen
import com.appvoyager.litememo.ui.theme.LiteMemoTheme
import com.appvoyager.litememo.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var appLockAuthenticator: AppLockAuthenticator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appLockAuthenticator = AppLockAuthenticator(this)
        enableEdgeToEdge()
        observeAuthenticationRequests()
        setContent {
            val themeMode by mainViewModel.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val appLockUiState by mainViewModel.appLockUiState.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LiteMemoTheme(darkTheme = darkTheme) {
                if (appLockUiState.canShowAppContent) {
                    LiteMemoApp(
                        onRequestAppLockAuthentication = { onResult ->
                            appLockAuthenticator.authenticate(onResult)
                        }
                    )
                } else {
                    AppLockScreen(
                        uiState = appLockUiState,
                        onUnlockClick = { mainViewModel.requestUnlock() }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mainViewModel.onAppStarted()
    }

    override fun onStop() {
        mainViewModel.onAppStopped()
        super.onStop()
    }

    private fun observeAuthenticationRequests() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.authenticationRequestEvent.collect {
                    appLockAuthenticator.authenticate { result ->
                        mainViewModel.onAuthenticationResult(result)
                    }
                }
            }
        }
    }
}
