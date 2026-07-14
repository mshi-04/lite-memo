package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.ExportData
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.provider.CurrentTimeProvider
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.repository.TagRepository
import javax.inject.Inject

class ExportMemosUseCase @Inject constructor(
    private val memoRepository: MemoRepository,
    private val tagRepository: TagRepository,
    private val currentTimeProvider: CurrentTimeProvider
) {

    suspend operator fun invoke(): ExportData {
        val memos = memoRepository.getAllActiveMemos().sortedMemosForExport()
        val tags = tagRepository.getAllTags().sortedTagsForExport()
        return ExportData(
            version = CURRENT_VERSION,
            exportedAt = currentTimeProvider.now(),
            tags = tags,
            memos = memos
        )
    }

    companion object {
        const val CURRENT_VERSION = 1
    }

}

private fun List<Memo>.sortedMemosForExport(): List<Memo> =
    sortedWith(compareBy<Memo> { it.createdAt.value }.thenBy { it.id.value })

private fun List<Tag>.sortedTagsForExport(): List<Tag> =
    sortedWith(compareBy<Tag> { it.createdAt.value }.thenBy { it.id.value })
