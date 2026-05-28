package com.appvoyager.litememo.domain.repository

import com.appvoyager.litememo.domain.model.MemoSortOrder
import com.appvoyager.litememo.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeUserSettingsRepository : UserSettingsRepository {

    private val themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    private val sortOrder = MutableStateFlow(MemoSortOrder.UPDATED_NEWEST)
    private val appLockEnabled = MutableStateFlow(false)

    override fun observeThemeMode(): Flow<ThemeMode> = themeMode

    override fun observeMemoSortOrder(): Flow<MemoSortOrder> = sortOrder

    override fun observeAppLockEnabled(): Flow<Boolean> = appLockEnabled

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }

    override suspend fun setMemoSortOrder(order: MemoSortOrder) {
        sortOrder.value = order
    }

    override suspend fun setAppLockEnabled(enabled: Boolean) {
        appLockEnabled.value = enabled
    }
}
