package com.appvoyager.litememo.data.repository

import com.appvoyager.litememo.data.export.MemoArchiveException
import com.appvoyager.litememo.data.export.MemoArchiveFailureReason
import com.appvoyager.litememo.data.export.MemoImportArchiveExtractor
import com.appvoyager.litememo.data.export.MemoImportSessionDataSource
import com.appvoyager.litememo.data.image.MemoImageFileDataSource
import com.appvoyager.litememo.data.local.dao.MemoDao
import com.appvoyager.litememo.domain.exception.MemoImportException
import com.appvoyager.litememo.domain.exception.MemoImportFailureReason
import com.appvoyager.litememo.domain.model.value.ExportFileReference
import com.appvoyager.litememo.domain.model.value.MemoImportSessionToken
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StagingMemoImportArchiveRepositoryTest {

    @Test
    fun coroutineStageCancellationRemainsPrimaryWhenRollbackFails() {
        // Arrange
        val reference = ExportFileReference("content://import.zip")
        val token = MemoImportSessionToken("session-1")
        val cancellation = CancellationException("cancelled")
        val rollbackFailure = IllegalStateException("rollback failed")
        val extractor = mockk<MemoImportArchiveExtractor>()
        val sessionDataSource = mockk<MemoImportSessionDataSource>()
        val imageFileDataSource = mockk<MemoImageFileDataSource>()
        coEvery { sessionDataSource.open() } returns token
        every { extractor.extractImages(reference, token) } throws cancellation
        coEvery {
            imageFileDataSource.listImageFileNamesStartingWith("${token.value}-")
        } throws rollbackFailure
        val repository = StagingMemoImportArchiveRepository(
            extractor = extractor,
            sessionDataSource = sessionDataSource,
            imageFileDataSource = imageFileDataSource,
            memoDao = mockk<MemoDao>(),
            ioDispatcher = UnconfinedTestDispatcher()
        )

        // Act
        // Coroutine: staging cancellation stays primary when cleanup also fails.
        val actual = assertThrows(CancellationException::class.java) {
            runTest { repository.stageImportImages(reference) }
        }

        // Assert
        assertAll(
            { assertSame(cancellation, actual) },
            { assertEquals(rollbackFailure.message, actual.suppressed.single().message) }
        )
    }

    @Test
    fun errorMappedArchiveFailureContainsRollbackFailureAsSuppressed() {
        // Arrange
        val reference = ExportFileReference("content://import.zip")
        val token = MemoImportSessionToken("session-1")
        val archiveFailure = MemoArchiveException(
            MemoArchiveFailureReason.MALFORMED_ARCHIVE,
            "invalid archive"
        )
        val rollbackFailure = IllegalStateException("rollback failed")
        val extractor = mockk<MemoImportArchiveExtractor>()
        val sessionDataSource = mockk<MemoImportSessionDataSource>()
        val imageFileDataSource = mockk<MemoImageFileDataSource>()
        coEvery { sessionDataSource.open() } returns token
        every { extractor.extractImages(reference, token) } throws archiveFailure
        coEvery {
            imageFileDataSource.listImageFileNamesStartingWith("${token.value}-")
        } throws rollbackFailure
        val repository = StagingMemoImportArchiveRepository(
            extractor = extractor,
            sessionDataSource = sessionDataSource,
            imageFileDataSource = imageFileDataSource,
            memoDao = mockk<MemoDao>(),
            ioDispatcher = UnconfinedTestDispatcher()
        )

        // Act
        // Error: cleanup diagnostics remain visible on the mapped exception callers receive.
        val actual = assertThrows(MemoImportException::class.java) {
            runTest { repository.stageImportImages(reference) }
        }

        // Assert
        assertAll(
            { assertEquals(MemoImportFailureReason.INVALID_ARCHIVE, actual.reason) },
            { assertSame(archiveFailure, actual.cause) },
            { assertEquals(rollbackFailure.message, actual.suppressed.single().message) }
        )
    }

}
