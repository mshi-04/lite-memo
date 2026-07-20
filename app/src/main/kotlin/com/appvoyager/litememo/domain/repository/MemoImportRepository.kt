package com.appvoyager.litememo.domain.repository

import com.appvoyager.litememo.domain.model.ExportData

interface MemoImportRepository {

    suspend fun import(data: ExportData)

}
