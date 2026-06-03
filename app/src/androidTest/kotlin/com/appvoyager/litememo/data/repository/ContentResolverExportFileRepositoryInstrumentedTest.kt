package com.appvoyager.litememo.data.repository

import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.appvoyager.litememo.data.export.ExportFileReader
import com.appvoyager.litememo.data.export.ExportFileWriter
import com.appvoyager.litememo.domain.model.ExportData
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.ExportFileReference
import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.MemoTitle
import com.appvoyager.litememo.domain.model.value.TagColor
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TagName
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ContentResolverExportFileRepositoryInstrumentedTest {

    private lateinit var context: Context
    private lateinit var file: File
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        file = File(context.cacheDir, "export-test-${System.nanoTime()}.json")
    }

    @After
    fun tearDown() {
        file.delete()
    }

    @Test
    fun readReturnsDataWrittenByWrite() = runTest {
        // Arrange
        val repository = repository(maxFileSizeBytes = DEFAULT_MAX_SIZE)
        val reference = reference()
        val data = exportData()

        // Act
        repository.write(reference, data)
        val restored = repository.read(reference)

        // Assert
        assertEquals(data, restored)
    }

    @Test
    fun readThrowsIOExceptionWhenValueRestorationFails() {
        // Arrange
        val repository = repository(maxFileSizeBytes = DEFAULT_MAX_SIZE)
        file.writeText(
            """
            {"version":1,"exportedAt":1000,"tags":[],
             "memos":[{"id":"memo-1","title":"T","body":"B",
             "createdAt":-1,"updatedAt":1000,"isFavorite":false,"tagIds":[]}]}
            """.trimIndent()
        )

        // Act & Assert
        assertThrows(IOException::class.java) {
            runTest { repository.read(reference()) }
        }
    }

    @Test
    fun readThrowsIOExceptionWhenFileExceedsSizeLimit() {
        // Arrange
        val writerRepository = repository(maxFileSizeBytes = DEFAULT_MAX_SIZE)
        val smallLimitRepository = repository(maxFileSizeBytes = 4L)

        // Act & Assert
        assertThrows(IOException::class.java) {
            runTest {
                writerRepository.write(reference(), exportData())
                smallLimitRepository.read(reference())
            }
        }
    }

    private fun reference(): ExportFileReference =
        ExportFileReference(Uri.fromFile(file).toString())

    private fun repository(maxFileSizeBytes: Long): ContentResolverExportFileRepository {
        val dispatcher = UnconfinedTestDispatcher()
        val writer = ExportFileWriter(context, json, dispatcher)
        val reader = ExportFileReader(context, json, dispatcher, maxFileSizeBytes)
        return ContentResolverExportFileRepository(writer, reader)
    }

    private fun exportData() = ExportData(
        version = 1,
        exportedAt = TimestampMillis(5000L),
        tags = listOf(
            Tag(
                id = TagId("tag-1"),
                name = TagName("Work"),
                color = TagColor(0xFF6750A4),
                createdAt = TimestampMillis(1000L)
            )
        ),
        memos = listOf(
            Memo(
                id = MemoId("memo-1"),
                title = MemoTitle("Title"),
                body = MemoBody("Body"),
                createdAt = TimestampMillis(2000L),
                updatedAt = TimestampMillis(3000L),
                tagIds = listOf(TagId("tag-1")),
                isFavorite = true,
                deletedAt = null
            )
        )
    )

    private companion object {
        const val DEFAULT_MAX_SIZE = 5L * 1024 * 1024
    }
}
