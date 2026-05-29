package com.appvoyager.litememo.domain.repository

import com.appvoyager.litememo.domain.model.MemoSortOrder
import com.appvoyager.litememo.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface UserSettingsRepository {

    fun observeThemeMode(): Flow<ThemeMode>

    fun observeMemoSortOrder(): Flow<MemoSortOrder>

    fun observeAppLockEnabled(): Flow<Boolean>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setMemoSortOrder(order: MemoSortOrder)

    suspend fun setAppLockEnabled(enabled: Boolean)
}
