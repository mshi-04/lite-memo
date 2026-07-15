package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.repository.UserSettingsRepository
import javax.inject.Inject

class SetAppLockEnabledUseCase @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository
) {

    suspend operator fun invoke(enabled: Boolean) =
        userSettingsRepository.setAppLockEnabled(enabled)
}
