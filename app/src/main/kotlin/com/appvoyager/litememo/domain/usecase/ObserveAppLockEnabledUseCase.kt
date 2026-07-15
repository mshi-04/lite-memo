package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.repository.UserSettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAppLockEnabledUseCase @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository
) {

    operator fun invoke(): Flow<Boolean> = userSettingsRepository.observeAppLockEnabled()

}
