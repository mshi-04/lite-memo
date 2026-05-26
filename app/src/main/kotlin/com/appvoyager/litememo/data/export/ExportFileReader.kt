package com.appvoyager.litememo.data.export

import android.content.Context
import android.net.Uri
import com.appvoyager.litememo.data.model.export.LiteMemoExportDto
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class ExportFileReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {

    suspend fun read(uri: Uri): LiteMemoExportDto = withContext(Dispatchers.IO) {
        val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader(Charsets.UTF_8).readText()
        } ?: throw IOException("Failed to open input stream for URI: $uri")
        json.decodeFromString<LiteMemoExportDto>(jsonString)
    }

}
