package com.appvoyager.litememo.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DataStoreUserSettingsRepositoryTest {

    @TempDir
    lateinit var tempDir: File

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

    private fun repository(scope: CoroutineScope): DataStoreUserSettingsRepository {
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            File(tempDir, "user_settings.preferences_pb")
        }
        return DataStoreUserSettingsRepository(dataStore)
    }
}
