package com.appvoyager.litememo.data.repository

import androidx.room.withTransaction
import com.appvoyager.litememo.data.local.LiteMemoDatabase
import com.appvoyager.litememo.data.local.dao.MemoDao
import com.appvoyager.litememo.data.mapper.toDomain
import com.appvoyager.litememo.data.mapper.toEntity
import com.appvoyager.litememo.data.mapper.toTagRefs
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.SearchQuery
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.repository.MemoRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomMemoRepository @Inject constructor(
    private val memoDao: MemoDao,
    private val database: LiteMemoDatabase
) : MemoRepository {

    override fun observeActiveMemos(): Flow<List<Memo>> =
        memoDao.observeActiveMemosWithTagRefs().map { memos ->
            memos.map { memo -> memo.toDomain() }
        }

    override fun observeActiveMemosBySearchQuery(query: SearchQuery): Flow<List<Memo>> =
        memoDao.observeActiveMemosWithTagRefsBySearchPattern(
            query.value.toEscapedLikePattern()
        ).map { memos ->
            memos.map { memo -> memo.toDomain() }
        }

    override fun observeActiveMemosCreatedBetween(
        from: TimestampMillis,
        to: TimestampMillis
    ): Flow<List<Memo>> {
        require(from.value < to.value) { "from must be earlier than to." }
        return memoDao
            .observeActiveMemosWithTagRefsCreatedBetween(from.value, to.value)
            .map { memos ->
                memos.map { memo -> memo.toDomain() }
            }
    }

    override fun observeTrashedMemos(): Flow<List<Memo>> =
        memoDao.observeTrashedMemosWithTagRefs().map { memos ->
            memos.map { memo -> memo.toDomain() }
        }

    override suspend fun getActiveMemo(id: MemoId): Memo? {
        val memo = memoDao.getActiveMemoWithTagRefs(id.value) ?: return null
        return memo.toDomain()
    }

    override suspend fun saveMemo(memo: Memo) {
        memoDao.upsertMemoWithTags(
            memo = memo.toEntity(),
            tagRefs = memo.toTagRefs()
        )
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
        val affected = memoDao.deleteMemoPermanently(id.value)
        check(affected > 0) { "Memo not found or not in trash: ${id.value}" }
    }

    override suspend fun deleteTrashedMemosDeletedAtOrBefore(cutoff: TimestampMillis) {
        memoDao.deleteTrashedMemosDeletedAtOrBefore(cutoff.value)
    }

    override suspend fun getAllActiveMemos(): List<Memo> =
        memoDao.getAllActiveMemosWithTagRefs().map { it.toDomain() }

    override suspend fun saveAllMemos(memos: List<Memo>) {
        val duplicateIds = memos.groupingBy { it.id.value }.eachCount()
            .filterValues { it > 1 }.keys
        require(duplicateIds.isEmpty()) { "Duplicate memo ids: $duplicateIds" }

        val entities = memos.map { it.toEntity() }
        val tagRefsByMemoId = memos.associate { memo ->
            memo.id.value to memo.toTagRefs()
        }
        memoDao.upsertAllMemosWithTags(entities, tagRefsByMemoId)
    }

    override suspend fun importAll(tags: List<Tag>, memos: List<Memo>) {
        val tagEntities = tags.map { it.toEntity() }

        val duplicateIds = memos.groupingBy { it.id.value }.eachCount()
            .filterValues { it > 1 }.keys
        require(duplicateIds.isEmpty()) { "Duplicate memo ids: $duplicateIds" }

        val entities = memos.map { it.toEntity() }
        val tagRefsByMemoId = memos.associate { memo ->
            memo.id.value to memo.toTagRefs()
        }

        database.withTransaction {
            database.tagDao().upsertAllTags(tagEntities)
            memoDao.upsertAllMemosWithTags(entities, tagRefsByMemoId)
        }
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
