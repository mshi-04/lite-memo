package com.appvoyager.litememo.data.export

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.appvoyager.litememo.data.di.IoDispatcher
import com.appvoyager.litememo.data.model.export.LiteMemoExportDto
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class ExportFileReader @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val json: Json,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @param:Named("importMaxFileSizeBytes") private val maxFileSizeBytes: Long
) {

    suspend fun read(uri: Uri): LiteMemoExportDto = withContext(ioDispatcher) {
        val size = fileSize(uri)
        if (size != null && size > maxFileSizeBytes) {
            throw IOException(
                "Import file too large: $size bytes exceeds limit of $maxFileSizeBytes bytes."
            )
        }
        val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader(Charsets.UTF_8).readText()
        } ?: throw IOException("Failed to open input stream for URI: $uri")
        try {
            json.decodeFromString<LiteMemoExportDto>(jsonString)
        } catch (e: SerializationException) {
            throw IOException("Failed to decode JSON from URI: $uri", e)
        }
    }

    /**
     * Returns the file size in bytes, or null if it cannot be determined.
     *
     * Prefers SAF's [OpenableColumns.SIZE] (the reliable source for `content://` URIs),
     * and falls back to the asset file descriptor length for schemes such as `file://`.
     */
    private fun fileSize(uri: Uri): Long? {
        val sizeFromColumns = context.contentResolver
            .query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst() && index >= 0 && !cursor.isNull(index)) {
                    cursor.getLong(index)
                } else {
                    null
                }
            }
        if (sizeFromColumns != null) return sizeFromColumns

        val length = context.contentResolver
            .openAssetFileDescriptor(uri, "r")
            ?.use { it.length }
        return length?.takeIf { it >= 0 }
    }

}
