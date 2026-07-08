package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.repository.UserSettingsRepository
import javax.inject.Inject

class CompleteTutorialUseCase @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository
) {
    suspend operator fun invoke() {
        userSettingsRepository.completeTutorial()
    }
}
