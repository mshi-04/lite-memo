package com.appvoyager.litememo.data.export

import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.appvoyager.litememo.data.model.export.LiteMemoExportDto
import com.appvoyager.litememo.data.model.export.MemoExportDto
import com.appvoyager.litememo.data.model.export.TagExportDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
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
class ExportFileWriterInstrumentedTest {

    private lateinit var context: Context
    private lateinit var file: File
    private var providerUriToDelete: Uri? = null
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        file = File(context.cacheDir, "writer-test-${System.nanoTime()}.json")
    }

    @After
    fun tearDown() {
        providerUriToDelete?.let { uri -> context.contentResolver.delete(uri, null, null) }
        file.delete()
    }

    @Test
    fun writeProducesJsonThatReaderCanRestore() = runTest {
        // Arrange
        val dispatcher = UnconfinedTestDispatcher()
        val writer = ExportFileWriter(context, json, dispatcher)
        val reader = ExportFileReader(context, json, dispatcher, DEFAULT_MAX_SIZE)
        val data = exportDto()

        // Act
        writer.write(Uri.fromFile(file), data)
        val restored = reader.read(Uri.fromFile(file))

        // Assert
        assertEquals(data, restored)
    }

    @Test
    fun normalWriteTruncatesExistingContentWhenOverwritingShorterJson() = runTest {
        // Arrange
        val writer = ExportFileWriter(context, json, UnconfinedTestDispatcher())
        val uri = Uri.parse("content://$NON_TRUNCATING_AUTHORITY/export-${System.nanoTime()}.json")
        providerUriToDelete = uri
        val shorterData = emptyExportDto()
        val expectedJson = json.encodeToString(shorterData)

        // Act
        // Normal: overwriting with shorter JSON truncates the previous content.
        writer.write(uri, exportDto())
        writer.write(uri, shorterData)

        // Assert
        assertEquals(expectedJson, readRawJson(uri))
    }

    @Test
    fun normalWriteFallsBackWhenProviderUriHasUnsafeFileName() = runTest {
        // Arrange
        val writer = ExportFileWriter(context, json, UnconfinedTestDispatcher())
        val unsafeUri = Uri.parse("content://$NON_TRUNCATING_AUTHORITY/unsafe%2Fexport.json")
        val fallbackUri = Uri.parse("content://$NON_TRUNCATING_AUTHORITY/export.json")
        providerUriToDelete = fallbackUri
        val data = emptyExportDto()
        val expectedJson = json.encodeToString(data)

        // Act
        // Normal: unsafe path segments are redirected to the provider fallback file.
        writer.write(unsafeUri, data)

        // Assert
        assertEquals(expectedJson, readRawJson(fallbackUri))
    }

    @Test
    fun writeThrowsIOExceptionWhenOutputStreamCannotOpen() {
        // Arrange
        val dispatcher = UnconfinedTestDispatcher()
        val writer = ExportFileWriter(context, json, dispatcher)
        val unwritable = File(context.cacheDir, "missing-dir-${System.nanoTime()}/out.json")

        // Act & Assert
        assertThrows(IOException::class.java) {
            runTest { writer.write(Uri.fromFile(unwritable), exportDto()) }
        }
    }

    private fun exportDto() = LiteMemoExportDto(
        version = 1,
        exportedAt = 5000L,
        tags = listOf(
            TagExportDto(
                id = "tag-1",
                name = "Work",
                colorArgb = 0xFF6750A4,
                createdAt = 1000L
            )
        ),
        memos = listOf(
            MemoExportDto(
                id = "memo-1",
                title = "Title",
                body = "Body",
                createdAt = 2000L,
                updatedAt = 3000L,
                isFavorite = true,
                tagIds = listOf("tag-1")
            )
        )
    )

    private fun emptyExportDto() = LiteMemoExportDto(
        version = 1,
        exportedAt = 6000L,
        tags = emptyList(),
        memos = emptyList()
    )

    private fun readRawJson(uri: Uri): String =
        requireNotNull(context.contentResolver.openInputStream(uri)) {
            "Failed to open input stream for URI: $uri"
        }.use { inputStream ->
            inputStream.bufferedReader(Charsets.UTF_8).readText()
        }

    private companion object {
        const val DEFAULT_MAX_SIZE = 5L * 1024 * 1024
        const val NON_TRUNCATING_AUTHORITY = "com.appvoyager.litememo.test.nontruncating"
    }
}
