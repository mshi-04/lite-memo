package com.appvoyager.litememo.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.appvoyager.litememo.domain.model.MemoSortOrder
import com.appvoyager.litememo.domain.model.ThemeMode
import com.appvoyager.litememo.domain.repository.UserSettingsRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DataStoreUserSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserSettingsRepository {

    private val preferencesFlow: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    override fun observeThemeMode(): Flow<ThemeMode> = preferencesFlow.map { prefs ->
        val name = prefs[THEME_MODE_KEY]
        name?.let { runCatching { enumValueOf<ThemeMode>(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    override fun observeMemoSortOrder(): Flow<MemoSortOrder> = preferencesFlow.map { prefs ->
        val name = prefs[MEMO_SORT_ORDER_KEY]
        name?.let { runCatching { enumValueOf<MemoSortOrder>(it) }.getOrNull() }
            ?: MemoSortOrder.UPDATED_NEWEST
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs -> prefs[THEME_MODE_KEY] = mode.name }
    }

    override suspend fun setMemoSortOrder(order: MemoSortOrder) {
        dataStore.edit { prefs -> prefs[MEMO_SORT_ORDER_KEY] = order.name }
    }

    private companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val MEMO_SORT_ORDER_KEY = stringPreferencesKey("memo_sort_order")
    }
}
