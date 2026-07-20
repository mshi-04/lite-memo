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

class ExportMemosToFileUseCaseTest {

    @Test
    fun interactionInvokeGeneratesDataBeforeWritingFile() = runTest {
        // Arrange
        val data = exportData()
        val reference = ExportFileReference("content://export")
        val exportMemosUseCase = mockk<ExportMemosUseCase>()
        val exportFileRepository = mockk<ExportFileRepository>()
        coEvery { exportMemosUseCase() } returns data
        coEvery { exportFileRepository.write(reference, data) } returns Unit
        val useCase = ExportMemosToFileUseCase(exportMemosUseCase, exportFileRepository)

        // Act
        // Interaction: data generation completes before the file is written once.
        useCase(reference)

        // Assert
        coVerifySequence {
            exportMemosUseCase()
            exportFileRepository.write(reference, data)
        }
    }

    @Test
    fun interactionGenerationFailureSkipsFileWrite() = runTest {
        // Arrange
        val error = IllegalStateException("generation failed")
        val exportMemosUseCase = mockk<ExportMemosUseCase>()
        val exportFileRepository = mockk<ExportFileRepository>(relaxed = true)
        coEvery { exportMemosUseCase() } throws error
        val useCase = ExportMemosToFileUseCase(exportMemosUseCase, exportFileRepository)

        // Act
        // Interaction/Error: failed data generation prevents any file write.
        runCatching { useCase(ExportFileReference("content://export")) }

        // Assert
        coVerify(exactly = 0) { exportFileRepository.write(any(), any()) }
    }

    @Test
    fun errorWriteFailurePropagatesToCaller() {
        // Arrange
        val data = exportData()
        val reference = ExportFileReference("content://export")
        val error = IllegalStateException("write failed")
        val exportMemosUseCase = mockk<ExportMemosUseCase>()
        val exportFileRepository = mockk<ExportFileRepository>()
        coEvery { exportMemosUseCase() } returns data
        coEvery { exportFileRepository.write(reference, data) } throws error
        val useCase = ExportMemosToFileUseCase(exportMemosUseCase, exportFileRepository)

        // Act
        // Error: the file write failure is not translated at the domain boundary.
        val actual = assertThrows(IllegalStateException::class.java) {
            runTest { useCase(reference) }
        }

        // Assert
        assertSame(error, actual)
    }

    @Test
    fun coroutineCancellationPropagatesToCaller() {
        // Arrange
        val cancellation = CancellationException("cancelled")
        val exportMemosUseCase = mockk<ExportMemosUseCase>()
        val exportFileRepository = mockk<ExportFileRepository>()
        coEvery { exportMemosUseCase() } throws cancellation
        val useCase = ExportMemosToFileUseCase(exportMemosUseCase, exportFileRepository)

        // Act
        // Coroutine: cancellation is never converted into an operation failure.
        val actual = assertThrows(CancellationException::class.java) {
            runTest { useCase(ExportFileReference("content://export")) }
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
