package com.appvoyager.litememo.domain.repository

import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.value.MemoId
import kotlinx.coroutines.flow.Flow

interface MemoRepository {

    fun observeMemos(): Flow<List<Memo>>

    suspend fun getMemo(id: MemoId): Memo?

    suspend fun saveMemo(memo: Memo)

    suspend fun saveMemoWithTagCheck(memo: Memo): Result<Unit>

    suspend fun deleteMemo(id: MemoId)
}
