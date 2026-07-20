package com.appvoyager.litememo.data.repository

import androidx.room.withTransaction
import com.appvoyager.litememo.data.local.LiteMemoDatabase
import com.appvoyager.litememo.data.local.dao.MemoDao
import com.appvoyager.litememo.data.local.dao.TagDao
import com.appvoyager.litememo.data.local.entity.MemoEntity
import com.appvoyager.litememo.data.local.entity.TagEntity
import com.appvoyager.litememo.data.mapper.toEntity
import com.appvoyager.litememo.data.mapper.toImageRefsByMemoId
import com.appvoyager.litememo.data.mapper.toTagRefsByMemoId
import com.appvoyager.litememo.data.util.deleteImageFiles
import com.appvoyager.litememo.data.util.requireNoDuplicateIds
import com.appvoyager.litememo.domain.model.ExportData
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
        data.tags.requireNoDuplicateIds(label = "tag") { it.id }
        data.memos.requireNoDuplicateIds(label = "memo") { it.id }

        val tagEntities: List<TagEntity> = data.tags.map { it.toEntity() }
        val memoEntities: List<MemoEntity> = data.memos.map { it.toEntity() }
        val tagRefsByMemoId = data.memos.toTagRefsByMemoId()
        val imageRefsByMemoId = data.memos.toImageRefsByMemoId()

        val removedFileNames = database.withTransaction {
            tagDao.upsertAllTags(tagEntities)
            memoDao.upsertAllMemosWithRefsAndCollectRemovedFileNames(
                memoEntities,
                tagRefsByMemoId,
                imageRefsByMemoId
            )
        }
        memoImageStore.deleteImageFiles(removedFileNames)
    }

}
