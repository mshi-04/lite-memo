package com.appvoyager.litememo.ui.viewmodel

import app.cash.turbine.test
import com.appvoyager.litememo.domain.FakeMemoImportRepository
import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.FakeTagRepository
import com.appvoyager.litememo.domain.MutableTimeProvider
import com.appvoyager.litememo.domain.exception.ImportTagNameConflictException
import com.appvoyager.litememo.domain.model.ExportData
import com.appvoyager.litememo.domain.model.value.ExportFileReference
import com.appvoyager.litememo.domain.model.value.TagName
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.repository.ExportFileRepository
import com.appvoyager.litememo.domain.repository.FakeUserSettingsRepository
import com.appvoyager.litememo.domain.usecase.ExportMemosToFileUseCase
import com.appvoyager.litememo.domain.usecase.ExportMemosUseCase
import com.appvoyager.litememo.domain.usecase.ImportMemosFromFileUseCase
import com.appvoyager.litememo.domain.usecase.ImportMemosUseCase
import com.appvoyager.litememo.domain.usecase.ObserveAppLockEnabledUseCase
import com.appvoyager.litememo.domain.usecase.ObserveMemoSortOrderUseCase
import com.appvoyager.litememo.domain.usecase.ObserveThemeModeUseCase
import com.appvoyager.litememo.domain.usecase.SetAppLockEnabledUseCase
import com.appvoyager.litememo.domain.usecase.SetMemoSortOrderUseCase
import com.appvoyager.litememo.domain.usecase.SetThemeModeUseCase
import com.appvoyager.litememo.ui.auth.AppLockAuthenticationUiResult
import com.appvoyager.litememo.ui.state.SettingsImportErrorDialogUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
    fun interactionExportMemosCallsFileOrchestrationOnce() = runTest(dispatcher) {
        // Arrange
        val reference = ExportFileReference("content://export")
        val exportUseCase = mockk<ExportMemosToFileUseCase>()
        val importUseCase = mockk<ImportMemosFromFileUseCase>()
        coEvery { exportUseCase(reference) } returns Unit
        val viewModel = settingsViewModel(exportUseCase, importUseCase)

        // Act
        // Interaction: the ViewModel delegates the whole export operation once.
        viewModel.exportMemos(reference)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { exportUseCase(reference) }
    }

    @Test
    fun interactionConfirmImportCallsFileOrchestrationOnce() = runTest(dispatcher) {
        // Arrange
        val reference = ExportFileReference("content://import")
        val exportUseCase = mockk<ExportMemosToFileUseCase>()
        val importUseCase = mockk<ImportMemosFromFileUseCase>()
        coEvery { importUseCase(reference) } returns Unit
        val viewModel = settingsViewModel(exportUseCase, importUseCase)
        viewModel.onImportFileSelected(reference)

        // Act
        // Interaction: confirmation delegates the whole import operation once.
        viewModel.confirmImport()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { importUseCase(reference) }
    }

    @Test
    fun stateTransitionExportMemosMaintainsProgressState() = runTest(dispatcher) {
        // Arrange
        val exportFileRepository = BlockingExportFileRepository()
        val viewModel = settingsViewModel(exportFileRepository)

        // Act
        // StateTransition: export progress stays active until the file operation finishes.
        viewModel.exportMemos(ExportFileReference("content://export"))
        runCurrent()
        val inProgress = viewModel.uiState.first { it.isExporting }.isExporting
        exportFileRepository.completeWrite()
        advanceUntilIdle()
        val completed = viewModel.uiState.first { !it.isExporting }.isExporting

        // Assert
        assertEquals(true to false, inProgress to completed)
    }

    @Test
    fun stateTransitionConfirmImportMaintainsProgressState() = runTest(dispatcher) {
        // Arrange
        val exportFileRepository = BlockingExportFileRepository()
        val viewModel = settingsViewModel(exportFileRepository)
        viewModel.onImportFileSelected(ExportFileReference("content://import"))

        // Act
        // StateTransition: import progress stays active until the file operation finishes.
        viewModel.confirmImport()
        runCurrent()
        val inProgress = viewModel.uiState.first { it.isImporting }.isImporting
        exportFileRepository.completeRead()
        advanceUntilIdle()
        val completed = viewModel.uiState.first { !it.isImporting }.isImporting

        // Assert
        assertEquals(true to false, inProgress to completed)
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
    fun flowExportMemosEmitsSuccessSnackbarWhenWriteSucceeds() = runTest(dispatcher) {
        // Arrange
        val exportFileRepository = ImmediateExportFileRepository()
        val viewModel = settingsViewModel(exportFileRepository)

        // Act & Assert
        // Flow/Normal: export success is emitted as a snackbar event.
        viewModel.snackbarEvent.test {
            viewModel.exportMemos(ExportFileReference("content://export"))
            advanceUntilIdle()
            assertEquals(SettingsSnackbarUiEvent.ExportSuccess, awaitItem())
        }
        assertEquals(
            listOf(ExportFileReference("content://export")),
            exportFileRepository.writtenReferences
        )
    }

    @Test
    fun flowExportMemosEmitsErrorSnackbarWhenWriteFails() = runTest(dispatcher) {
        // Arrange
        val viewModel = settingsViewModel(
            ImmediateExportFileRepository(writeError = IllegalStateException("write failed"))
        )

        // Act & Assert
        // Flow/Error: failed export is converted to an error snackbar.
        viewModel.snackbarEvent.test {
            viewModel.exportMemos(ExportFileReference("content://export"))
            advanceUntilIdle()
            assertEquals(SettingsSnackbarUiEvent.ExportError, awaitItem())
        }
    }

    @Test
    fun flowConfirmImportEmitsSuccessSnackbarWhenReadSucceeds() = runTest(dispatcher) {
        // Arrange
        val exportFileRepository = ImmediateExportFileRepository()
        val viewModel = settingsViewModel(exportFileRepository)
        viewModel.onImportFileSelected(ExportFileReference("content://import"))
        advanceUntilIdle()

        // Act & Assert
        // Flow/Normal/StateTransition: import confirmation closes the dialog and emits success.
        viewModel.snackbarEvent.test {
            viewModel.confirmImport()
            advanceUntilIdle()
            assertEquals(SettingsSnackbarUiEvent.ImportSuccess, awaitItem())
        }
        assertEquals(false, viewModel.uiState.value.showImportConfirmDialog)
        assertEquals(
            listOf(ExportFileReference("content://import")),
            exportFileRepository.readReferences
        )
    }

    @Test
    fun flowConfirmImportShowsGenericErrorDialogWithoutSnackbarWhenReadFails() =
        runTest(dispatcher) {
            // Arrange
            val viewModel = settingsViewModel(
                ImmediateExportFileRepository(readError = IllegalStateException("read failed"))
            )
            viewModel.onImportFileSelected(ExportFileReference("content://import"))
            advanceUntilIdle()

            // Act & Assert
            // Flow/Error: failed import is held as dialog state instead of a snackbar.
            viewModel.snackbarEvent.test {
                viewModel.confirmImport()
                advanceUntilIdle()
                expectNoEvents()
            }
            val state = viewModel.uiState.first { it.importErrorDialog != null }

            // Assert
            assertEquals(SettingsImportErrorDialogUiState.Generic, state.importErrorDialog)
        }

    @Test
    fun flowConfirmImportShowsAllConflictingTagNamesWhenImportRejectsTagNames() =
        runTest(dispatcher) {
            // Arrange
            val reference = ExportFileReference("content://import")
            val importUseCase = mockk<ImportMemosFromFileUseCase>()
            coEvery { importUseCase(reference) } throws ImportTagNameConflictException(
                listOf(TagName("Home"), TagName("Work"))
            )
            val viewModel = settingsViewModel(mockk<ExportMemosToFileUseCase>(), importUseCase)
            viewModel.onImportFileSelected(reference)
            advanceUntilIdle()

            // Act & Assert
            // Flow/Error: tag name conflicts are surfaced as a dialog holding every name.
            viewModel.snackbarEvent.test {
                viewModel.confirmImport()
                advanceUntilIdle()
                expectNoEvents()
            }
            val state = viewModel.uiState.first { it.importErrorDialog != null }

            // Assert
            assertEquals(
                SettingsImportErrorDialogUiState.TagNameConflict(listOf("Home", "Work")),
                state.importErrorDialog
            )
        }

    @Test
    fun stateTransitionDismissImportErrorDialogClearsState() = runTest(dispatcher) {
        // Arrange
        val viewModel = settingsViewModel(
            ImmediateExportFileRepository(readError = IllegalStateException("read failed"))
        )
        viewModel.onImportFileSelected(ExportFileReference("content://import"))
        viewModel.confirmImport()
        advanceUntilIdle()
        viewModel.uiState.first { it.importErrorDialog != null }

        // Act
        // StateTransition: dismissing the dialog releases the retained failure.
        viewModel.dismissImportErrorDialog()
        advanceUntilIdle()
        val state = viewModel.uiState.first { it.importErrorDialog == null }

        // Assert
        assertEquals(null, state.importErrorDialog)
    }

    @Test
    fun boundaryConfirmImportDoesNotReadWhenNoImportFileIsPending() = runTest(dispatcher) {
        // Arrange
        val exportFileRepository = ImmediateExportFileRepository()
        val viewModel = settingsViewModel(exportFileRepository)

        // Act & Assert
        // Flow/Boundary: confirming without a selected file is a no-op and emits no snackbar.
        viewModel.snackbarEvent.test {
            viewModel.confirmImport()
            advanceUntilIdle()
            expectNoEvents()
        }

        // Assert
        assertEquals(emptyList<ExportFileReference>(), exportFileRepository.readReferences)
    }

    @Test
    fun boundaryDismissImportDialogClearsPendingReferenceBeforeConfirm() = runTest(dispatcher) {
        // Arrange
        val exportFileRepository = ImmediateExportFileRepository()
        val viewModel = settingsViewModel(exportFileRepository)
        viewModel.onImportFileSelected(ExportFileReference("content://import"))
        advanceUntilIdle()
        viewModel.uiState.first { it.showImportConfirmDialog }

        // Act & Assert
        // Flow/Boundary/Interaction: dismissed import selection cannot be confirmed later.
        viewModel.snackbarEvent.test {
            viewModel.dismissImportConfirmDialog()
            viewModel.confirmImport()
            advanceUntilIdle()
            expectNoEvents()
        }

        // Assert
        assertEquals(
            false to emptyList<ExportFileReference>(),
            viewModel.uiState.value.showImportConfirmDialog to exportFileRepository.readReferences
        )
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
        viewModel.onAppLockEnableAuthenticationResult(AppLockAuthenticationUiResult.SUCCEEDED)
        advanceUntilIdle()

        // Assert
        assertEquals(true, userSettingsRepository.observeAppLockEnabled().first())
    }

    @Test
    fun flowAppLockNoDeviceCredentialEmitsSnackbarAndKeepsDisabled() = runTest(dispatcher) {
        // Arrange
        val userSettingsRepository = FakeUserSettingsRepository()
        val viewModel = settingsViewModel(
            exportFileRepository = BlockingExportFileRepository(),
            userSettingsRepository = userSettingsRepository
        )

        // Act & Assert
        viewModel.snackbarEvent.test {
            viewModel.onAppLockEnableAuthenticationResult(
                AppLockAuthenticationUiResult.NO_DEVICE_CREDENTIAL
            )
            advanceUntilIdle()
            assertEquals(SettingsSnackbarUiEvent.AppLockNoDeviceCredential, awaitItem())
        }
        assertEquals(false, userSettingsRepository.observeAppLockEnabled().first())
    }

    @Test
    fun flowAppLockUnavailableEmitsSnackbarAndKeepsDisabled() = runTest(dispatcher) {
        // Arrange
        val userSettingsRepository = FakeUserSettingsRepository()
        val viewModel = settingsViewModel(
            exportFileRepository = BlockingExportFileRepository(),
            userSettingsRepository = userSettingsRepository
        )

        // Act & Assert
        viewModel.snackbarEvent.test {
            viewModel.onAppLockEnableAuthenticationResult(AppLockAuthenticationUiResult.UNAVAILABLE)
            advanceUntilIdle()
            assertEquals(SettingsSnackbarUiEvent.AppLockUnavailable, awaitItem())
        }
        assertEquals(false, userSettingsRepository.observeAppLockEnabled().first())
    }

    @Test
    fun flowAppLockFailedEmitsFailedSnackbarAndKeepsDisabled() = runTest(dispatcher) {
        // Arrange
        val userSettingsRepository = FakeUserSettingsRepository()
        val viewModel = settingsViewModel(
            exportFileRepository = BlockingExportFileRepository(),
            userSettingsRepository = userSettingsRepository
        )

        // Act & Assert
        viewModel.snackbarEvent.test {
            viewModel.onAppLockEnableAuthenticationResult(AppLockAuthenticationUiResult.FAILED)
            advanceUntilIdle()
            assertEquals(SettingsSnackbarUiEvent.AppLockAuthenticationFailed, awaitItem())
        }
        assertEquals(false, userSettingsRepository.observeAppLockEnabled().first())
    }

    @Test
    fun flowAppLockCanceledEmitsCanceledSnackbarAndKeepsDisabled() = runTest(dispatcher) {
        // Arrange
        val userSettingsRepository = FakeUserSettingsRepository()
        val viewModel = settingsViewModel(
            exportFileRepository = BlockingExportFileRepository(),
            userSettingsRepository = userSettingsRepository
        )

        // Act & Assert
        viewModel.snackbarEvent.test {
            viewModel.onAppLockEnableAuthenticationResult(AppLockAuthenticationUiResult.CANCELED)
            advanceUntilIdle()
            assertEquals(SettingsSnackbarUiEvent.AppLockAuthenticationCanceled, awaitItem())
        }
        assertEquals(false, userSettingsRepository.observeAppLockEnabled().first())
    }

    @Test
    fun expandThemeDropdownCollapsesSortOrderDropdown() = runTest(dispatcher) {
        // Arrange
        val viewModel = settingsViewModel(BlockingExportFileRepository())
        viewModel.expandSortOrder()
        advanceUntilIdle()

        // Act
        viewModel.expandThemeDropdown()
        advanceUntilIdle()
        val state = viewModel.uiState.first { it.themeDropdownExpanded }

        // Assert
        assertEquals(true to false, state.themeDropdownExpanded to state.sortOrderExpanded)
    }

    @Test
    fun expandSortOrderCollapsesThemeDropdown() = runTest(dispatcher) {
        // Arrange
        val viewModel = settingsViewModel(BlockingExportFileRepository())
        viewModel.expandThemeDropdown()
        advanceUntilIdle()

        // Act
        viewModel.expandSortOrder()
        advanceUntilIdle()
        val state = viewModel.uiState.first { it.sortOrderExpanded }

        // Assert
        assertEquals(false to true, state.themeDropdownExpanded to state.sortOrderExpanded)
    }

    private fun settingsViewModel(exportFileRepository: ExportFileRepository): SettingsViewModel =
        settingsViewModel(
            exportFileRepository = exportFileRepository,
            userSettingsRepository = FakeUserSettingsRepository()
        )

    private fun settingsViewModel(
        exportUseCase: ExportMemosToFileUseCase,
        importUseCase: ImportMemosFromFileUseCase
    ): SettingsViewModel {
        val userSettingsRepository = FakeUserSettingsRepository()
        return SettingsViewModel(
            observeThemeModeUseCase = ObserveThemeModeUseCase(userSettingsRepository),
            setThemeModeUseCase = SetThemeModeUseCase(userSettingsRepository),
            observeMemoSortOrderUseCase = ObserveMemoSortOrderUseCase(userSettingsRepository),
            setMemoSortOrderUseCase = SetMemoSortOrderUseCase(userSettingsRepository),
            observeAppLockEnabledUseCase = ObserveAppLockEnabledUseCase(userSettingsRepository),
            setAppLockEnabledUseCase = SetAppLockEnabledUseCase(userSettingsRepository),
            exportMemosToFileUseCase = exportUseCase,
            importMemosFromFileUseCase = importUseCase,
            appVersion = "1.0.0"
        )
    }

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
            exportMemosToFileUseCase = ExportMemosToFileUseCase(
                exportMemosUseCase = ExportMemosUseCase(
                    memoRepository = memoRepository,
                    tagRepository = tagRepository,
                    currentTimeProvider = MutableTimeProvider(TimestampMillis(1_000L))
                ),
                exportFileRepository = exportFileRepository
            ),
            importMemosFromFileUseCase = ImportMemosFromFileUseCase(
                exportFileRepository = exportFileRepository,
                importMemosUseCase = ImportMemosUseCase(
                    memoImportRepository = FakeMemoImportRepository()
                )
            ),
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

    private class ImmediateExportFileRepository(
        private val readError: Throwable? = null,
        private val writeError: Throwable? = null
    ) : ExportFileRepository {

        val writtenReferences = mutableListOf<ExportFileReference>()
        val readReferences = mutableListOf<ExportFileReference>()

        override suspend fun write(reference: ExportFileReference, data: ExportData) {
            writeError?.let { throw it }
            writtenReferences += reference
        }

        override suspend fun read(reference: ExportFileReference): ExportData {
            readError?.let { throw it }
            readReferences += reference
            return ExportData(
                version = ExportMemosUseCase.CURRENT_VERSION,
                exportedAt = TimestampMillis(1_000L),
                tags = emptyList(),
                memos = emptyList()
            )
        }
    }
}
