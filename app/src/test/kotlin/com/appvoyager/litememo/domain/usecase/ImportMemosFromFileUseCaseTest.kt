package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.FakeMemoImportArchiveRepository
import com.appvoyager.litememo.domain.FakeMemoImportRepository
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.ExportData
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.StagedMemoImport
import com.appvoyager.litememo.domain.model.value.ExportFileReference
import com.appvoyager.litememo.domain.model.value.MemoImportSessionToken
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.repository.ExportFileRepository
import com.appvoyager.litememo.domain.repository.MemoImportArchiveRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ImportMemosFromFileUseCaseTest {

    @Test
    fun interactionInvokeReadsFileBeforeApplyingData() = runTest {
        // Arrange
        val data = exportData()
        val reference = ExportFileReference("content://import")
        val exportFileRepository = mockk<ExportFileRepository>()
        val importMemosUseCase = mockk<ImportMemosUseCase>()
        coEvery { exportFileRepository.read(reference) } returns data
        coEvery { importMemosUseCase(data) } returns Unit
        val useCase = useCase(
            exportFileRepository = exportFileRepository,
            archiveRepository = FakeMemoImportArchiveRepository(isArchive = false),
            importMemosUseCase = importMemosUseCase
        )

        // Act
        // Interaction: file reading completes before validation and application run once.
        useCase(reference)

        // Assert
        coVerifySequence {
            exportFileRepository.read(reference)
            importMemosUseCase(data)
        }
    }

    @Test
    fun interactionReadFailureSkipsDataApplication() = runTest {
        // Arrange
        val exportFileRepository = mockk<ExportFileRepository>()
        val importMemosUseCase = mockk<ImportMemosUseCase>(relaxed = true)
        coEvery { exportFileRepository.read(any()) } throws IllegalStateException("read failed")
        val useCase = useCase(
            exportFileRepository = exportFileRepository,
            archiveRepository = FakeMemoImportArchiveRepository(isArchive = false),
            importMemosUseCase = importMemosUseCase
        )

        // Act
        // Interaction/Error: a file read failure prevents DB import.
        runCatching { useCase(ExportFileReference("content://import")) }

        // Assert
        coVerify(exactly = 0) { importMemosUseCase(any()) }
    }

    @Test
    fun errorApplicationFailurePropagatesToCaller() {
        // Arrange
        val data = exportData()
        val reference = ExportFileReference("content://import")
        val error = IllegalArgumentException("invalid data")
        val exportFileRepository = mockk<ExportFileRepository>()
        val importMemosUseCase = mockk<ImportMemosUseCase>()
        coEvery { exportFileRepository.read(reference) } returns data
        coEvery { importMemosUseCase(data) } throws error
        val useCase = useCase(
            exportFileRepository = exportFileRepository,
            archiveRepository = FakeMemoImportArchiveRepository(isArchive = false),
            importMemosUseCase = importMemosUseCase
        )

        // Act
        // Error: validation and application failures remain visible to the caller.
        val actual = assertThrows(IllegalArgumentException::class.java) {
            runTest { useCase(reference) }
        }

        // Assert
        assertSame(error, actual)
    }

    @Test
    fun coroutineCancellationPropagatesToCaller() {
        // Arrange
        val reference = ExportFileReference("content://import")
        val cancellation = CancellationException("cancelled")
        val exportFileRepository = mockk<ExportFileRepository>()
        val importMemosUseCase = mockk<ImportMemosUseCase>()
        coEvery { exportFileRepository.read(reference) } throws cancellation
        val useCase = useCase(
            exportFileRepository = exportFileRepository,
            archiveRepository = FakeMemoImportArchiveRepository(isArchive = false),
            importMemosUseCase = importMemosUseCase
        )

        // Act
        // Coroutine: cancellation from file reading is not converted into an import failure.
        val actual = assertThrows(CancellationException::class.java) {
            runTest { useCase(reference) }
        }

        // Assert
        assertSame(cancellation, actual)
    }

    @Test
    fun normalInvokeImportsStagedArchiveDataWhenReferenceIsArchive() = runTest {
        // Arrange
        val data = exportData(memos = listOf(memoFixture(id = "m1")))
        val importRepository = FakeMemoImportRepository()
        val useCase = archiveUseCase(archiveRepository(data), importRepository)

        // Act
        // Normal: archive input is staged and the staged data reaches persistence.
        useCase(ExportFileReference("content://import.zip"))

        // Assert
        assertEquals(data, importRepository.importedData.single())
    }

    @Test
    fun interactionInvokeCompletesStagedImportAfterSuccessfulPersistence() = runTest {
        // Arrange
        val archiveRepository = archiveRepository(exportData(memos = listOf(memoFixture())))
        val useCase = archiveUseCase(archiveRepository, FakeMemoImportRepository())

        // Act
        // Interaction: the staging session is completed only once persistence succeeded.
        useCase(ExportFileReference("content://import.zip"))

        // Assert
        assertEquals(
            listOf(FakeMemoImportArchiveRepository.TOKEN) to emptyList<MemoImportSessionToken>(),
            archiveRepository.completedTokens to archiveRepository.rolledBackTokens
        )
    }

    @Test
    fun errorInvokeRollsBackStagedImagesWhenPersistenceFails() = runTest {
        // Arrange
        val archiveRepository = archiveRepository(exportData(memos = listOf(memoFixture())))
        val importMemosUseCase = mockk<ImportMemosUseCase>()
        coEvery { importMemosUseCase(any()) } throws IllegalStateException("db failed")
        val useCase = useCase(
            exportFileRepository = mockk(),
            archiveRepository = archiveRepository,
            importMemosUseCase = importMemosUseCase
        )

        // Act
        // Error: a persistence failure rolls the staged images back and never completes.
        runCatching { useCase(ExportFileReference("content://import.zip")) }

        // Assert
        assertEquals(
            listOf(FakeMemoImportArchiveRepository.TOKEN) to emptyList<MemoImportSessionToken>(),
            archiveRepository.rolledBackTokens to archiveRepository.completedTokens
        )
    }

    @Test
    fun errorArchivePersistenceFailureRemainsPrimaryWhenRollbackFails() {
        // Arrange
        val reference = ExportFileReference("content://import.zip")
        val data = exportData(memos = listOf(memoFixture()))
        val persistenceFailure = IllegalStateException("db failed")
        val rollbackFailure = IllegalStateException("rollback failed")
        val archiveRepository = mockk<MemoImportArchiveRepository>()
        val importMemosUseCase = mockk<ImportMemosUseCase>()
        coEvery { archiveRepository.isArchive(reference) } returns true
        coEvery { archiveRepository.stageImportImages(reference) } returns StagedMemoImport(
            token = FakeMemoImportArchiveRepository.TOKEN,
            data = data
        )
        coEvery { importMemosUseCase(data) } throws persistenceFailure
        coEvery {
            archiveRepository.rollbackStagedImport(FakeMemoImportArchiveRepository.TOKEN)
        } throws rollbackFailure
        val useCase = useCase(mockk(), archiveRepository, importMemosUseCase)

        // Act
        // Error: rollback diagnostics do not replace the persistence failure.
        val actual = assertThrows(IllegalStateException::class.java) {
            runTest { useCase(reference) }
        }

        // Assert
        assertAll(
            { assertSame(persistenceFailure, actual) },
            { assertEquals(rollbackFailure.message, actual.suppressed.single().message) }
        )
    }

    @Test
    fun coroutineArchiveCancellationRemainsPrimaryWhenRollbackFails() {
        // Arrange
        val reference = ExportFileReference("content://import.zip")
        val data = exportData(memos = listOf(memoFixture()))
        val cancellation = CancellationException("cancelled")
        val rollbackFailure = IllegalStateException("rollback failed")
        val archiveRepository = mockk<MemoImportArchiveRepository>()
        val importMemosUseCase = mockk<ImportMemosUseCase>()
        coEvery { archiveRepository.isArchive(reference) } returns true
        coEvery { archiveRepository.stageImportImages(reference) } returns StagedMemoImport(
            token = FakeMemoImportArchiveRepository.TOKEN,
            data = data
        )
        coEvery { importMemosUseCase(data) } throws cancellation
        coEvery {
            archiveRepository.rollbackStagedImport(FakeMemoImportArchiveRepository.TOKEN)
        } throws rollbackFailure
        val useCase = useCase(mockk(), archiveRepository, importMemosUseCase)

        // Act
        // Coroutine: archive cancellation is rethrown after best-effort rollback.
        val actual = assertThrows(CancellationException::class.java) {
            runTest { useCase(reference) }
        }

        // Assert
        assertAll(
            { assertSame(cancellation, actual) },
            { assertEquals(rollbackFailure.message, actual.suppressed.single().message) }
        )
    }

    @Test
    fun errorInvokeSkipsPersistenceWhenStagingFails() = runTest {
        // Arrange
        val archiveRepository = archiveRepository(exportData())
        archiveRepository.stageError = IllegalStateException("staging failed")
        val importRepository = FakeMemoImportRepository()
        val useCase = archiveUseCase(archiveRepository, importRepository)

        // Act
        // Error/Interaction: a staging failure never starts the DB import.
        runCatching { useCase(ExportFileReference("content://import.zip")) }

        // Assert
        assertEquals(emptyList<ExportData>(), importRepository.importedData)
    }

    private fun archiveRepository(data: ExportData) = FakeMemoImportArchiveRepository(
        isArchive = true,
        stagedData = data
    )

    private fun archiveUseCase(
        archiveRepository: MemoImportArchiveRepository,
        importRepository: FakeMemoImportRepository
    ) = useCase(
        exportFileRepository = mockk(),
        archiveRepository = archiveRepository,
        importMemosUseCase = ImportMemosUseCase(memoImportRepository = importRepository)
    )

    private fun useCase(
        exportFileRepository: ExportFileRepository,
        archiveRepository: MemoImportArchiveRepository,
        importMemosUseCase: ImportMemosUseCase
    ) = ImportMemosFromFileUseCase(
        exportFileRepository = exportFileRepository,
        memoImportArchiveRepository = archiveRepository,
        importMemosUseCase = importMemosUseCase
    )

    private fun exportData(memos: List<Memo> = emptyList()) = ExportData(
        version = ExportMemosUseCase.CURRENT_VERSION,
        exportedAt = TimestampMillis(1_000L),
        tags = emptyList(),
        memos = memos
    )

}
