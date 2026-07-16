package com.appvoyager.litememo.data.repository

import androidx.room.withTransaction
import com.appvoyager.litememo.data.local.LiteMemoDatabase
import com.appvoyager.litememo.data.local.dao.MemoDao
import com.appvoyager.litememo.data.local.dao.TagDao
import com.appvoyager.litememo.data.mapper.toDomain
import com.appvoyager.litememo.data.mapper.toEntity
import com.appvoyager.litememo.data.mapper.toImageRefs
import com.appvoyager.litememo.data.mapper.toTagRefs
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.MemoImageFileName
import com.appvoyager.litememo.domain.model.value.SearchQuery
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.repository.MemoImageStore
import com.appvoyager.litememo.domain.repository.MemoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomMemoRepository @Inject constructor(
    private val memoDao: MemoDao,
    private val tagDao: TagDao,
    private val database: LiteMemoDatabase,
    private val memoImageStore: MemoImageStore
) : MemoRepository {

    override fun observeActiveMemos(): Flow<List<Memo>> =
        memoDao.observeActiveMemosWithRefs().map { memos ->
            memos.map { memo -> memo.toDomain() }
        }

    override fun observeActiveMemosBySearchQuery(query: SearchQuery): Flow<List<Memo>> =
        memoDao.observeActiveMemosWithRefsBySearchPattern(
            query.value.toEscapedLikePattern()
        ).map { memos ->
            memos.map { memo -> memo.toDomain() }
        }

    override fun observeActiveMemosCreatedBetween(
        from: TimestampMillis,
        to: TimestampMillis
    ): Flow<List<Memo>> {
        require(from.value <= to.value) { "from must not be later than to." }
        return memoDao
            .observeActiveMemosWithRefsCreatedBetween(from.value, to.value)
            .map { memos ->
                memos.map { memo -> memo.toDomain() }
            }
    }

    override fun observeTrashedMemos(): Flow<List<Memo>> =
        memoDao.observeTrashedMemosWithRefs().map { memos ->
            memos.map { memo -> memo.toDomain() }
        }

    override suspend fun getActiveMemo(id: MemoId): Memo? {
        val memo = memoDao.getActiveMemoWithRefs(id.value) ?: return null
        return memo.toDomain()
    }

    override suspend fun saveMemo(memo: Memo) {
        val removedFileNames = memoDao.upsertMemoWithRefsAndCollectRemovedFileNames(
            memo = memo.toEntity(),
            tagRefs = memo.toTagRefs(),
            imageRefs = memo.toImageRefs()
        )
        deleteImageFiles(removedFileNames)
    }

    override suspend fun moveMemoToTrash(id: MemoId, deletedAt: TimestampMillis) {
        val affected = memoDao.moveMemoToTrash(id.value, deletedAt.value)
        check(affected > 0) { "Memo not found or already trashed: ${id.value}" }
    }

    override suspend fun restoreMemoFromTrash(id: MemoId) {
        val affected = memoDao.restoreMemoFromTrash(id.value)
        check(affected > 0) { "Memo not found or not in trash: ${id.value}" }
    }

    override suspend fun deleteMemoPermanently(id: MemoId) {
        val fileNames = memoDao.deleteMemoPermanentlyAndCollectImageFileNames(id.value)
        deleteImageFiles(fileNames)
    }

    override suspend fun discardMemo(id: MemoId) {
        val fileNames = memoDao.discardMemoAndCollectImageFileNames(id.value)
        deleteImageFiles(fileNames)
    }

    override suspend fun deleteTrashedMemosDeletedAtOrBefore(cutoff: TimestampMillis) {
        val fileNames = memoDao.deleteTrashedMemosDeletedAtOrBeforeAndCollectImageFileNames(
            cutoff.value
        )
        deleteImageFiles(fileNames)
    }

    override suspend fun getAllActiveMemos(): List<Memo> =
        memoDao.getAllActiveMemosWithRefs().map { it.toDomain() }

    override suspend fun saveAllMemos(memos: List<Memo>) {
        requireNoDuplicateMemoIds(memos)

        val entities = memos.map { it.toEntity() }
        val tagRefsByMemoId = memos.associate { memo ->
            memo.id.value to memo.toTagRefs()
        }
        val imageRefsByMemoId = memos.associate { memo ->
            memo.id.value to memo.toImageRefs()
        }
        val removedFileNames = memoDao.upsertAllMemosWithRefsAndCollectRemovedFileNames(
            entities,
            tagRefsByMemoId,
            imageRefsByMemoId
        )
        deleteImageFiles(removedFileNames)
    }

    override suspend fun importAll(tags: List<Tag>, memos: List<Memo>) {
        val removedFileNames = database.withTransaction {
            executeImport(tags, memos)
        }
        deleteImageFiles(removedFileNames)
    }

    suspend fun executeImport(tags: List<Tag>, memos: List<Memo>): List<String> {
        requireNoDuplicateTagIds(tags)
        requireNoDuplicateMemoIds(memos)

        val tagEntities = tags.map { it.toEntity() }
        val entities = memos.map { it.toEntity() }
        val tagRefsByMemoId = memos.associate { memo ->
            memo.id.value to memo.toTagRefs()
        }
        val imageRefsByMemoId = memos.associate { memo ->
            memo.id.value to memo.toImageRefs()
        }

        tagDao.upsertAllTags(tagEntities)
        return memoDao.upsertAllMemosWithRefsAndCollectRemovedFileNames(
            entities,
            tagRefsByMemoId,
            imageRefsByMemoId
        )
    }

    private fun requireNoDuplicateMemoIds(memos: List<Memo>) {
        val duplicateIds = memos.groupingBy { it.id.value }.eachCount()
            .filterValues { it > 1 }.keys
        require(duplicateIds.isEmpty()) { "Duplicate memo ids: $duplicateIds" }
    }

    private fun requireNoDuplicateTagIds(tags: List<Tag>) {
        val duplicateIds = tags.groupingBy { it.id.value }.eachCount()
            .filterValues { it > 1 }.keys
        require(duplicateIds.isEmpty()) { "Duplicate tag ids: $duplicateIds" }
    }

    private suspend fun deleteImageFiles(fileNames: Collection<String>) {
        if (fileNames.isEmpty()) return
        memoImageStore.deleteImages(fileNames.map { MemoImageFileName(it) })
    }

    private fun String.toEscapedLikePattern(): String = buildString {
        append(LIKE_MULTI_CHARACTER_WILDCARD)
        this@toEscapedLikePattern.forEach { char ->
            when (char) {
                LIKE_ESCAPE_CHARACTER,
                LIKE_MULTI_CHARACTER_WILDCARD,
                LIKE_SINGLE_CHARACTER_WILDCARD -> append(LIKE_ESCAPE_CHARACTER)
            }
            append(char)
        }
        append(LIKE_MULTI_CHARACTER_WILDCARD)
    }

    private companion object {
        const val LIKE_ESCAPE_CHARACTER = '\\'
        const val LIKE_MULTI_CHARACTER_WILDCARD = '%'
        const val LIKE_SINGLE_CHARACTER_WILDCARD = '_'
    }

}
