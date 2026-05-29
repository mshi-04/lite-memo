package com.appvoyager.litememo

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
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
        observeSecureScreen()
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
                        onUnlockClick = { mainViewModel.requestUnlock() },
                        onOpenSecuritySettings = { openSecuritySettings() }
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

    private fun observeSecureScreen() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.secureScreenEnabled.collect { enabled ->
                    if (enabled) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }
        }
    }

    private fun openSecuritySettings() {
        runCatching { startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
            .onFailure { startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }
}
