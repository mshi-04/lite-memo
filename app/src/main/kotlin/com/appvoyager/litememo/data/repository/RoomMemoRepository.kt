package com.appvoyager.litememo.data.repository

import com.appvoyager.litememo.data.local.dao.MemoDao
import com.appvoyager.litememo.data.mapper.toDomain
import com.appvoyager.litememo.data.mapper.toEntity
import com.appvoyager.litememo.data.mapper.toTagRefs
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.SearchQuery
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.repository.MemoRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class RoomMemoRepository @Inject constructor(private val memoDao: MemoDao) : MemoRepository {

    override fun observeActiveMemos(): Flow<List<Memo>> =
        memoDao.observeMemosWithTagRefs().map { memos ->
            memos.map { memo -> memo.toDomain() }
        }

    override fun observeActiveMemosBySearchQuery(query: SearchQuery): Flow<List<Memo>> =
        memoDao.observeMemosWithTagRefsBySearchPattern(
            query.value.toEscapedLikePattern()
        ).map { memos ->
            memos.map { memo -> memo.toDomain() }
        }

    override fun observeActiveMemosCreatedBetween(
        from: TimestampMillis,
        to: TimestampMillis
    ): Flow<List<Memo>> {
        require(from.value < to.value) { "from must be earlier than to." }
        return memoDao.observeMemosWithTagRefsCreatedBetween(from.value, to.value).map { memos ->
            memos.map { memo -> memo.toDomain() }
        }
    }

    override fun observeTrashedMemos(): Flow<List<Memo>> = flowOf(emptyList())

    override suspend fun getActiveMemo(id: MemoId): Memo? {
        val memo = memoDao.getMemoWithTagRefs(id.value) ?: return null
        return memo.toDomain()
    }

    override suspend fun saveMemo(memo: Memo) {
        memoDao.upsertMemoWithTags(
            memo = memo.toEntity(),
            tagRefs = memo.toTagRefs()
        )
    }

    override suspend fun moveMemoToTrash(id: MemoId, deletedAt: TimestampMillis) {
        // no-op: DAO の deletedAt 対応は feature/trash-data で実装
    }

    override suspend fun restoreMemoFromTrash(id: MemoId) {
        // no-op: DAO の deletedAt 対応は feature/trash-data で実装
    }

    override suspend fun deleteMemoPermanently(id: MemoId) {
        // no-op: DAO の deletedAt 対応は feature/trash-data で実装
    }

    override suspend fun deleteTrashedMemosDeletedAtOrBefore(cutoff: TimestampMillis) {
        // no-op: DAO の deletedAt 対応は feature/trash-data で実装
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
