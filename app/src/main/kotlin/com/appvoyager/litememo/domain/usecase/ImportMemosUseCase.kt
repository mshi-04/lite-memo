package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.ExportData
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.repository.TagRepository
import javax.inject.Inject

class ImportMemosUseCase @Inject constructor(
    private val memoRepository: MemoRepository,
    private val tagRepository: TagRepository
) {

    suspend operator fun invoke(data: ExportData) {
        require(data.version == ExportMemosUseCase.CURRENT_VERSION) {
            "Unsupported export version: ${data.version}."
        }

        tagRepository.saveAllTags(data.tags)

        val validTagIds = data.tags.map { it.id }.toSet()
        val sanitizedMemos = data.memos.map { memo ->
            val filtered = memo.tagIds.filter { it in validTagIds }
            if (filtered.size == memo.tagIds.size) memo else memo.copy(tagIds = filtered)
        }

        memoRepository.saveAllMemos(sanitizedMemos)
    }

}
