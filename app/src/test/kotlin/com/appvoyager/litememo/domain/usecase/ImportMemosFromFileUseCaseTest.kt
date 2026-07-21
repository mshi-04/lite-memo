package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.ExportData
import com.appvoyager.litememo.domain.model.value.ExportFileReference
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.repository.ExportFileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
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
        val useCase = ImportMemosFromFileUseCase(exportFileRepository, importMemosUseCase)

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
        val useCase = ImportMemosFromFileUseCase(exportFileRepository, importMemosUseCase)

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
        val useCase = ImportMemosFromFileUseCase(exportFileRepository, importMemosUseCase)

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
        val useCase = ImportMemosFromFileUseCase(exportFileRepository, importMemosUseCase)

        // Act
        // Coroutine: cancellation from file reading is not converted into an import failure.
        val actual = assertThrows(CancellationException::class.java) {
            runTest { useCase(reference) }
        }

        // Assert
        assertSame(cancellation, actual)
    }

    private fun exportData() = ExportData(
        version = ExportMemosUseCase.CURRENT_VERSION,
        exportedAt = TimestampMillis(1_000L),
        tags = emptyList(),
        memos = emptyList()
    )

}
