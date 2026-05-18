package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.appvoyager.litememo.domain.model.ThemeMode
import com.appvoyager.litememo.domain.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository
) : ViewModel() {

    val themeMode: Flow<ThemeMode> = userSettingsRepository.observeThemeMode()
}
