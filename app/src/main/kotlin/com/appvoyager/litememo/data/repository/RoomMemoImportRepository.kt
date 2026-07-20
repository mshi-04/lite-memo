package com.appvoyager.litememo.data.repository

import androidx.room.withTransaction
import com.appvoyager.litememo.data.local.LiteMemoDatabase
import com.appvoyager.litememo.data.local.dao.MemoDao
import com.appvoyager.litememo.data.local.dao.TagDao
import com.appvoyager.litememo.data.mapper.toEntity
import com.appvoyager.litememo.data.mapper.toImageRefs
import com.appvoyager.litememo.data.mapper.toTagRefs
import com.appvoyager.litememo.domain.model.ExportData
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.MemoImageFileName
import com.appvoyager.litememo.domain.repository.MemoImageStore
import com.appvoyager.litememo.domain.repository.MemoImportRepository
import javax.inject.Inject

class RoomMemoImportRepository @Inject constructor(
    private val memoDao: MemoDao,
    private val tagDao: TagDao,
    private val database: LiteMemoDatabase,
    private val memoImageStore: MemoImageStore
) : MemoImportRepository {

    override suspend fun import(data: ExportData) {
        requireNoDuplicateTagIds(data.tags)
        requireNoDuplicateMemoIds(data.memos)

        val removedFileNames = database.withTransaction {
            val tagEntities = data.tags.map { it.toEntity() }
            val memoEntities = data.memos.map { it.toEntity() }
            val tagRefsByMemoId = data.memos.associate { memo ->
                memo.id.value to memo.toTagRefs()
            }
            val imageRefsByMemoId = data.memos.associate { memo ->
                memo.id.value to memo.toImageRefs()
            }

            tagDao.upsertAllTags(tagEntities)
            memoDao.upsertAllMemosWithRefsAndCollectRemovedFileNames(
                memoEntities,
                tagRefsByMemoId,
                imageRefsByMemoId
            )
        }
        deleteImageFiles(removedFileNames)
    }

    private fun requireNoDuplicateMemoIds(memos: List<Memo>) {
        val duplicateIds = memos.groupingBy { it.id }.eachCount()
            .filterValues { it > 1 }.keys
        require(duplicateIds.isEmpty()) { "Duplicate memo ids: $duplicateIds" }
    }

    private fun requireNoDuplicateTagIds(tags: List<Tag>) {
        val duplicateIds = tags.groupingBy { it.id }.eachCount()
            .filterValues { it > 1 }.keys
        require(duplicateIds.isEmpty()) { "Duplicate tag ids: $duplicateIds" }
    }

    private suspend fun deleteImageFiles(fileNames: Collection<String>) {
        if (fileNames.isEmpty()) return
        memoImageStore.deleteImages(fileNames.map { MemoImageFileName(it) })
    }

}
