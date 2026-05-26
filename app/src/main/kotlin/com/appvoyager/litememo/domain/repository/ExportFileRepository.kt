package com.appvoyager.litememo.domain.repository

import com.appvoyager.litememo.domain.model.ExportData
import com.appvoyager.litememo.domain.model.value.ExportFileReference

interface ExportFileRepository {
    suspend fun write(reference: ExportFileReference, data: ExportData)
    suspend fun read(reference: ExportFileReference): ExportData
}
