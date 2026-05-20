package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.MemoSortOrder
import com.appvoyager.litememo.domain.repository.UserSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveMemoSortOrderUseCase @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository
) {

    operator fun invoke(): Flow<MemoSortOrder> = userSettingsRepository.observeMemoSortOrder()
}
