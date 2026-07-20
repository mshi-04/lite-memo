package com.appvoyager.litememo.domain.repository

import com.appvoyager.litememo.domain.model.ExportData

interface MemoImportRepository {

    // 実装は重複した memo id / tag id を含む data を書き込み前に拒否し、同一 id の既存メモは上書きする。
    suspend fun import(data: ExportData)

}
