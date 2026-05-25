package com.appvoyager.litememo.domain.model

import com.appvoyager.litememo.domain.model.value.TimestampMillis

data class ExportData(
    val version: Int,
    val exportedAt: TimestampMillis,
    val tags: List<Tag>,
    val memos: List<Memo>
) {

    init {
        require(version > 0) { "ExportData version must be positive." }
    }

}
