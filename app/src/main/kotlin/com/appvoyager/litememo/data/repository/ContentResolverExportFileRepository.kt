package com.appvoyager.litememo.data.repository

import android.net.Uri
import com.appvoyager.litememo.data.export.ExportFileReader
import com.appvoyager.litememo.data.export.ExportFileWriter
import com.appvoyager.litememo.data.mapper.toDomain
import com.appvoyager.litememo.data.mapper.toDto
import com.appvoyager.litememo.domain.model.ExportData
import com.appvoyager.litememo.domain.model.value.ExportFileReference
import com.appvoyager.litememo.domain.repository.ExportFileRepository
import javax.inject.Inject

class ContentResolverExportFileRepository @Inject constructor(
    private val exportFileWriter: ExportFileWriter,
    private val exportFileReader: ExportFileReader
) : ExportFileRepository {

    override suspend fun write(reference: ExportFileReference, data: ExportData) {
        exportFileWriter.write(reference.toUri(), data.toDto())
    }

    override suspend fun read(reference: ExportFileReference): ExportData =
        exportFileReader.read(reference.toUri()).toDomain()

    private fun ExportFileReference.toUri(): Uri = Uri.parse(value)
}
