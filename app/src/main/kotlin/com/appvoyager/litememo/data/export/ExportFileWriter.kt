package com.appvoyager.litememo.data.export

import android.content.Context
import android.net.Uri
import com.appvoyager.litememo.data.di.IoDispatcher
import com.appvoyager.litememo.data.model.export.LiteMemoExportDto
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ExportFileWriter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun write(uri: Uri, data: LiteMemoExportDto) {
        withContext(ioDispatcher) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val jsonString = json.encodeToString(data)
                    outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                } ?: throw IOException("Failed to open output stream for URI: $uri")
            } catch (e: SerializationException) {
                throw IOException("Failed to encode data to JSON for URI: $uri", e)
            }
        }
    }

}
