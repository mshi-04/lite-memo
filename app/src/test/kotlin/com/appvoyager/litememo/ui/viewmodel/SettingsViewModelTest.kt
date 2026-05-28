package com.appvoyager.litememo.ui.viewmodel

import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.FakeTagRepository
import com.appvoyager.litememo.domain.MutableTimeProvider
import com.appvoyager.litememo.domain.model.ExportData
import com.appvoyager.litememo.domain.model.value.ExportFileReference
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.repository.ExportFileRepository
import com.appvoyager.litememo.domain.repository.FakeUserSettingsRepository
import com.appvoyager.litememo.domain.usecase.ExportMemosUseCase
import com.appvoyager.litememo.domain.usecase.ImportMemosUseCase
import com.appvoyager.litememo.domain.usecase.ObserveAppLockEnabledUseCase
import com.appvoyager.litememo.domain.usecase.ObserveMemoSortOrderUseCase
import com.appvoyager.litememo.domain.usecase.ObserveThemeModeUseCase
import com.appvoyager.litememo.domain.usecase.SetAppLockEnabledUseCase
import com.appvoyager.litememo.domain.usecase.SetMemoSortOrderUseCase
import com.appvoyager.litememo.domain.usecase.SetThemeModeUseCase
import com.appvoyager.litememo.ui.auth.AppLockAuthenticationResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var dispatcher: TestDispatcher

    @BeforeEach
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun exportMemosDoesNotStartWhenImportIsRunning() = runTest(dispatcher) {
        // Arrange
        val exportFileRepository = BlockingExportFileRepository()
        val viewModel = settingsViewModel(exportFileRepository)
        try {
            viewModel.onImportFileSelected(ExportFileReference("content://import"))
            viewModel.confirmImport()
            runCurrent()

            // Act
            viewModel.exportMemos(ExportFileReference("content://export"))
            runCurrent()

            // Assert
            assertEquals(emptyList<ExportFileReference>(), exportFileRepository.writtenReferences)
        } finally {
            exportFileRepository.completeRead()
            exportFileRepository.completeWrite()
            advanceUntilIdle()
        }
    }

    @Test
    fun confirmImportDoesNotStartWhenExportIsRunning() = runTest(dispatcher) {
        // Arrange
        val exportFileRepository = BlockingExportFileRepository()
        val viewModel = settingsViewModel(exportFileRepository)
        try {
            viewModel.onImportFileSelected(ExportFileReference("content://import"))
            viewModel.exportMemos(ExportFileReference("content://export"))
            runCurrent()

            // Act
            viewModel.confirmImport()
            runCurrent()

            // Assert
            assertEquals(emptyList<ExportFileReference>(), exportFileRepository.readReferences)
        } finally {
            exportFileRepository.completeRead()
            exportFileRepository.completeWrite()
            advanceUntilIdle()
        }
    }

    @Test
    fun appLockAuthenticationSuccessEnablesAppLock() = runTest(dispatcher) {
        // Arrange
        val userSettingsRepository = FakeUserSettingsRepository()
        val viewModel = settingsViewModel(
            exportFileRepository = BlockingExportFileRepository(),
            userSettingsRepository = userSettingsRepository
        )

        // Act
        viewModel.onAppLockEnableAuthenticationResult(AppLockAuthenticationResult.SUCCEEDED)
        advanceUntilIdle()

        // Assert
        assertEquals(true, userSettingsRepository.observeAppLockEnabled().first())
    }

    private fun settingsViewModel(exportFileRepository: ExportFileRepository): SettingsViewModel =
        settingsViewModel(
            exportFileRepository = exportFileRepository,
            userSettingsRepository = FakeUserSettingsRepository()
        )

    private fun settingsViewModel(
        exportFileRepository: ExportFileRepository,
        userSettingsRepository: FakeUserSettingsRepository
    ): SettingsViewModel {
        val memoRepository = FakeMemoRepository()
        val tagRepository = FakeTagRepository()
        return SettingsViewModel(
            observeThemeModeUseCase = ObserveThemeModeUseCase(userSettingsRepository),
            setThemeModeUseCase = SetThemeModeUseCase(userSettingsRepository),
            observeMemoSortOrderUseCase = ObserveMemoSortOrderUseCase(userSettingsRepository),
            setMemoSortOrderUseCase = SetMemoSortOrderUseCase(userSettingsRepository),
            observeAppLockEnabledUseCase = ObserveAppLockEnabledUseCase(userSettingsRepository),
            setAppLockEnabledUseCase = SetAppLockEnabledUseCase(userSettingsRepository),
            exportMemosUseCase = ExportMemosUseCase(
                memoRepository = memoRepository,
                tagRepository = tagRepository,
                currentTimeProvider = MutableTimeProvider(TimestampMillis(1_000L))
            ),
            importMemosUseCase = ImportMemosUseCase(
                memoRepository = memoRepository,
                tagRepository = tagRepository
            ),
            exportFileRepository = exportFileRepository,
            appVersion = "1.0.0"
        )
    }

    private class BlockingExportFileRepository : ExportFileRepository {

        private val writeCompleted = CompletableDeferred<Unit>()
        private val readCompleted = CompletableDeferred<Unit>()

        val writtenReferences = mutableListOf<ExportFileReference>()
        val readReferences = mutableListOf<ExportFileReference>()

        override suspend fun write(reference: ExportFileReference, data: ExportData) {
            writtenReferences += reference
            writeCompleted.await()
        }

        override suspend fun read(reference: ExportFileReference): ExportData {
            readReferences += reference
            readCompleted.await()
            return ExportData(
                version = ExportMemosUseCase.CURRENT_VERSION,
                exportedAt = TimestampMillis(1_000L),
                tags = emptyList(),
                memos = emptyList()
            )
        }

        fun completeWrite() {
            writeCompleted.complete(Unit)
        }

        fun completeRead() {
            readCompleted.complete(Unit)
        }
    }
}
