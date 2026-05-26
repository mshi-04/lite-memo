package com.appvoyager.litememo.data.model.export

import kotlinx.serialization.Serializable

@Serializable
data class LiteMemoExportDto(
    val version: Int,
    val exportedAt: Long,
    val tags: List<TagExportDto>,
    val memos: List<MemoExportDto>
)
