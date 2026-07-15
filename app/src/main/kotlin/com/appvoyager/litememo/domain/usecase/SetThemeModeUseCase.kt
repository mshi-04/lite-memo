package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.ThemeMode
import com.appvoyager.litememo.domain.repository.UserSettingsRepository
import javax.inject.Inject

class SetThemeModeUseCase @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository
) {

    suspend operator fun invoke(mode: ThemeMode) = userSettingsRepository.setThemeMode(mode)
}
