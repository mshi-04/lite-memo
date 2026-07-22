package com.appvoyager.litememo.data.export

import android.content.Context
import com.appvoyager.litememo.data.di.IoDispatcher
import com.appvoyager.litememo.domain.model.value.MemoImportSessionToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoImportSessionDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    private val openTokens = ConcurrentHashMap.newKeySet<String>()

    suspend fun open(): MemoImportSessionToken = withContext(ioDispatcher) {
        val token = MemoImportSessionToken(UUID.randomUUID().toString())
        markerDir().mkdirs()
        if (!markerFile(token).createNewFile()) {
            throw IOException("Import session marker already exists.")
        }
        openTokens += token.value
        stagingDir(token).mkdirs()
        token
    }

    suspend fun close(token: MemoImportSessionToken) {
        withContext(ioDispatcher) {
            stagingDir(token).deleteRecursively()
            markerFile(token).delete()
            openTokens -= token.value
        }
    }

    suspend fun abandonedTokens(): List<MemoImportSessionToken> = withContext(ioDispatcher) {
        markerDir().listFiles().orEmpty()
            .filter { it.isFile && it.name !in openTokens }
            .mapNotNull { file -> runCatching { MemoImportSessionToken(file.name) }.getOrNull() }
    }

    fun stagedImageFile(token: MemoImportSessionToken, archiveEntry: String): File =
        File(stagingDir(token), archiveEntry.substringAfterLast(ARCHIVE_ENTRY_SEPARATOR))

    private fun stagingDir(token: MemoImportSessionToken): File =
        File(File(context.cacheDir, STAGING_DIR), token.value)

    private fun markerFile(token: MemoImportSessionToken): File = File(markerDir(), token.value)

    private fun markerDir(): File = File(context.filesDir, MARKER_DIR)

    private companion object {
        const val STAGING_DIR = "import_staging"
        const val MARKER_DIR = "import_sessions"
        const val ARCHIVE_ENTRY_SEPARATOR = '/'
    }

}
