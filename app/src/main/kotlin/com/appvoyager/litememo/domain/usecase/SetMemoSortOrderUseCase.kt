package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.MemoSortOrder
import com.appvoyager.litememo.domain.repository.UserSettingsRepository
import javax.inject.Inject

class SetMemoSortOrderUseCase @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository
) {

    suspend operator fun invoke(order: MemoSortOrder) {
        userSettingsRepository.setMemoSortOrder(order)
    }
}
