package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.ThemeMode
import com.appvoyager.litememo.domain.repository.UserSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveThemeModeUseCase @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository
) {

    operator fun invoke(): Flow<ThemeMode> = userSettingsRepository.observeThemeMode()
}
