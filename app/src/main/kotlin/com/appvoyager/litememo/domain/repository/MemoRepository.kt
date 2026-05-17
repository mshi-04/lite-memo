package com.appvoyager.litememo.domain.repository

import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import kotlinx.coroutines.flow.Flow

interface MemoRepository {

    fun observeMemos(): Flow<List<Memo>>

    fun observeMemosCreatedBetween(from: TimestampMillis, to: TimestampMillis): Flow<List<Memo>>

    suspend fun getMemo(id: MemoId): Memo?

    suspend fun saveMemo(memo: Memo)

    suspend fun deleteMemo(id: MemoId)

}
