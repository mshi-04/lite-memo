package com.appvoyager.litememo.data.repository

import com.appvoyager.litememo.data.local.dao.MemoDao
import com.appvoyager.litememo.data.local.entity.MemoTagRefEntity
import com.appvoyager.litememo.data.mapper.toDomain
import com.appvoyager.litememo.data.mapper.toEntity
import com.appvoyager.litememo.data.mapper.toTagRefs
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.repository.MemoRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RoomMemoRepository @Inject constructor(private val memoDao: MemoDao) : MemoRepository {

    override fun observeMemos(): Flow<List<Memo>> = combine(
        memoDao.observeMemos(),
        memoDao.observeMemoTagRefs()
    ) { memos, tagRefs ->
        val tagRefsByMemoId = tagRefs.groupBy(MemoTagRefEntity::memoId)
        memos.map { memo ->
            memo.toDomain(tagRefsByMemoId[memo.id].orEmpty())
        }
    }

    override suspend fun getMemo(id: MemoId): Memo? {
        val memo = memoDao.getMemo(id.value) ?: return null
        return memo.toDomain(memoDao.getTagRefsForMemo(id.value))
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
