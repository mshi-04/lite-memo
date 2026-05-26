package com.appvoyager.litememo.domain.repository

import com.appvoyager.litememo.domain.model.ExportData
import com.appvoyager.litememo.domain.model.value.ExportFileReference

interface ExportFileRepository {
    /**
     * 指定された参照へエクスポートデータを書き込む。
     *
     * @throws java.io.IOException ファイル書き込みに失敗した場合
     * @throws SecurityException 参照先への権限がない場合
     */
    suspend fun write(reference: ExportFileReference, data: ExportData)

    /**
     * 指定された参照からエクスポートデータを読み込む。
     *
     * @throws java.io.IOException ファイル読み込みに失敗した場合
     * @throws SecurityException 参照先への権限がない場合
     * @throws kotlinx.serialization.SerializationException 読み込んだデータを解析できない場合
     */
    suspend fun read(reference: ExportFileReference): ExportData
}
