package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.exception.ImportTagNameConflictException
import com.appvoyager.litememo.domain.model.ExportData
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.TagName
import com.appvoyager.litememo.domain.repository.MemoImportRepository
import javax.inject.Inject

class ImportMemosUseCase @Inject constructor(
    private val memoImportRepository: MemoImportRepository
) {

    suspend operator fun invoke(data: ExportData) {
        require(data.version == ExportMemosUseCase.CURRENT_VERSION) {
            "Unsupported export version: ${data.version}."
        }

        val conflictingTagNames = data.tags.conflictingTagNames()
        if (conflictingTagNames.isNotEmpty()) {
            throw ImportTagNameConflictException(conflictingTagNames)
        }

        val validTagIds = data.tags.map { it.id }.toSet()
        val sanitizedMemos = data.memos.map { memo ->
            val filtered = memo.tagIds.filter { it in validTagIds }.distinct()
            if (filtered.size == memo.tagIds.size) memo else memo.copy(tagIds = filtered)
        }
        memoImportRepository.import(data.copy(memos = sanitizedMemos))
    }

}

private fun List<Tag>.conflictingTagNames(): List<TagName> = groupBy { it.name }
    .filterValues { tags -> tags.distinctBy { it.id }.size > 1 }
    .keys
    .sortedBy { it.value }
