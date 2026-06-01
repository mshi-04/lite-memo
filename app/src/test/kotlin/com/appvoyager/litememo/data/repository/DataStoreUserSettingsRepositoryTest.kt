package com.appvoyager.litememo.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.appvoyager.litememo.domain.model.MemoSortOrder
import com.appvoyager.litememo.domain.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DataStoreUserSettingsRepositoryTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun observeThemeModeReturnsDefaultValue() = runTest {
        // Arrange
        val repository = repository(backgroundScope)

        // Act
        val result = repository.observeThemeMode().first()

        // Assert
        assertEquals(ThemeMode.SYSTEM, result)
    }

    @Test
    fun observeThemeModeReturnsSavedValue() = runTest {
        // Arrange
        val repository = repository(backgroundScope)

        // Act
        repository.setThemeMode(ThemeMode.DARK)
        val result = repository.observeThemeMode().first()

        // Assert
        assertEquals(ThemeMode.DARK, result)
    }

    // 不正な保存値（enum 名が変わった等）でもデフォルトにフォールバックすることを検証する
    @Test
    fun observeThemeModeReturnsDefaultWhenStoredValueIsInvalid() = runTest {
        // Arrange
        val dataStore = dataStore(backgroundScope)
        dataStore.edit { prefs ->
            prefs[DataStoreUserSettingsRepository.THEME_MODE_KEY] = "NOT_A_VALID_MODE"
        }
        val repository = DataStoreUserSettingsRepository(dataStore)

        // Act
        val result = repository.observeThemeMode().first()

        // Assert
        assertEquals(ThemeMode.SYSTEM, result)
    }

    @Test
    fun observeMemoSortOrderReturnsDefaultValue() = runTest {
        // Arrange
        val repository = repository(backgroundScope)

        // Act
        val result = repository.observeMemoSortOrder().first()

        // Assert
        assertEquals(MemoSortOrder.UPDATED_NEWEST, result)
    }

    @Test
    fun observeMemoSortOrderReturnsSavedValue() = runTest {
        // Arrange
        val repository = repository(backgroundScope)

        // Act
        repository.setMemoSortOrder(MemoSortOrder.CREATED_NEWEST)
        val result = repository.observeMemoSortOrder().first()

        // Assert
        assertEquals(MemoSortOrder.CREATED_NEWEST, result)
    }

    // 不正な保存値でもデフォルトにフォールバックすることを検証する
    @Test
    fun observeMemoSortOrderReturnsDefaultWhenStoredValueIsInvalid() = runTest {
        // Arrange
        val dataStore = dataStore(backgroundScope)
        dataStore.edit { prefs ->
            prefs[DataStoreUserSettingsRepository.MEMO_SORT_ORDER_KEY] = "NOT_A_VALID_ORDER"
        }
        val repository = DataStoreUserSettingsRepository(dataStore)

        // Act
        val result = repository.observeMemoSortOrder().first()

        // Assert
        assertEquals(MemoSortOrder.UPDATED_NEWEST, result)
    }

    @Test
    fun observeAppLockEnabledReturnsDefaultValue() = runTest {
        // Arrange
        val repository = repository(backgroundScope)

        // Act
        val result = repository.observeAppLockEnabled().first()

        // Assert
        assertEquals(false, result)
    }

    @Test
    fun observeAppLockEnabledReturnsSavedValue() = runTest {
        // Arrange
        val repository = repository(backgroundScope)

        // Act
        repository.setAppLockEnabled(true)
        val result = repository.observeAppLockEnabled().first()

        // Assert
        assertEquals(true, result)
    }

    private fun repository(scope: CoroutineScope): DataStoreUserSettingsRepository =
        DataStoreUserSettingsRepository(dataStore(scope))

    private fun dataStore(scope: CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope) {
            File(tempDir, "user_settings.preferences_pb")
        }
}
