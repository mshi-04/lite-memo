package com.appvoyager.litememo.data.export

import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ExportFileReaderInstrumentedTest {

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
        file = File(context.cacheDir, "reader-test-${System.nanoTime()}.json")
    }

    @After
    fun tearDown() {
        file.delete()
    }

    // SerializationException を IOException に変換する分岐を検証する。
    // （壊れた JSON は値オブジェクト変換ではなくデコード段階で失敗する）
    @Test
    fun readThrowsIOExceptionWhenJsonIsMalformed() {
        // Arrange
        val reader = reader(DEFAULT_MAX_SIZE)
        file.writeText("{ this is not valid json ")

        // Act & Assert
        assertThrows(IOException::class.java) {
            runTest { reader.read(Uri.fromFile(file)) }
        }
    }

    // 注: ストリーミング上限経路 (readUtf8WithLimit) は file:// URI では
    // fileSize() の asset file descriptor フォールバックが必ず実サイズを返すため、
    // 事前チェックで先に弾かれる。pre-check 経路のサイズ上限は
    // ContentResolverExportFileRepositoryInstrumentedTest で検証済み。

    private fun reader(maxFileSizeBytes: Long): ExportFileReader =
        ExportFileReader(context, json, UnconfinedTestDispatcher(), maxFileSizeBytes)

    private companion object {
        const val DEFAULT_MAX_SIZE = 5L * 1024 * 1024
    }
}
