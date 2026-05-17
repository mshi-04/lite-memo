package com.appvoyager.litememo.data.repository

import com.appvoyager.litememo.data.local.dao.MemoDao
import com.appvoyager.litememo.data.mapper.toDomain
import com.appvoyager.litememo.data.mapper.toEntity
import com.appvoyager.litememo.data.mapper.toTagRefs
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.repository.MemoRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomMemoRepository @Inject constructor(private val memoDao: MemoDao) : MemoRepository {

    override fun observeMemos(): Flow<List<Memo>> = memoDao.observeMemosWithTagRefs().map { memos ->
        memos.map { memo -> memo.toDomain() }
    }

    override fun observeMemosCreatedBetween(
        from: TimestampMillis,
        to: TimestampMillis
    ): Flow<List<Memo>> {
        require(from.value < to.value) { "from must be earlier than to." }
        return memoDao.observeMemosWithTagRefsCreatedBetween(from.value, to.value).map { memos ->
            memos.map { memo -> memo.toDomain() }
        }
    }

    override suspend fun getMemo(id: MemoId): Memo? {
        val memo = memoDao.getMemoWithTagRefs(id.value) ?: return null
        return memo.toDomain()
    }

    override suspend fun saveMemo(memo: Memo) {
        memoDao.upsertMemoWithTags(
            memo = memo.toEntity(),
            tagRefs = memo.toTagRefs()
        )
    }

    override suspend fun deleteMemo(id: MemoId) {
        memoDao.deleteMemo(id.value)
    }

}
