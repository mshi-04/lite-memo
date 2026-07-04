package com.appvoyager.litememo.data.export

import android.content.Context
import android.net.Uri
import com.appvoyager.litememo.data.di.IoDispatcher
import com.appvoyager.litememo.data.model.export.LiteMemoExportDto
import com.appvoyager.litememo.di.ExportJson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject

class ExportFileWriter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:ExportJson private val json: Json,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun write(uri: Uri, data: LiteMemoExportDto) {
        withContext(ioDispatcher) {
            try {
                val jsonString = json.encodeToString(data)
                context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                } ?: throw IOException("Failed to open output stream for URI: $uri")
            } catch (e: SerializationException) {
                throw IOException("Failed to encode data to JSON for URI: $uri", e)
            }
        }
    }

}
