package com.appvoyager.litememo.domain.repository

import com.appvoyager.litememo.domain.model.ExportData

interface MemoImportRepository {

    // 実装は重複した memo id / tag id を含む data を書き込み前に拒否し、同一 id の既存メモは上書きする。
    // import で上書きされない既存 Tag と同名・別 ID の衝突が 1 件でもあれば、書き込みを始めずに
    // 全衝突名を持つ ImportTagNameConflictException を投げ、import 全体を rollback する。
    suspend fun import(data: ExportData)

}
