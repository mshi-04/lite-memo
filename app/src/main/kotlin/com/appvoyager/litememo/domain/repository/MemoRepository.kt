package com.appvoyager.litememo.domain.repository

import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.SearchQuery
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import kotlinx.coroutines.flow.Flow

interface MemoRepository {

    fun observeActiveMemos(): Flow<List<Memo>>

    fun observeActiveMemosBySearchQuery(query: SearchQuery): Flow<List<Memo>>

    fun observeActiveMemosCreatedBetween(
        from: TimestampMillis,
        to: TimestampMillis
    ): Flow<List<Memo>>

    fun observeTrashedMemos(): Flow<List<Memo>>

    suspend fun getActiveMemo(id: MemoId): Memo?

    suspend fun saveMemo(memo: Memo)

    suspend fun moveMemoToTrash(id: MemoId, deletedAt: TimestampMillis)

    suspend fun restoreMemoFromTrash(id: MemoId)

    suspend fun deleteMemoPermanently(id: MemoId)

    suspend fun deleteTrashedMemosDeletedAtOrBefore(cutoff: TimestampMillis)

    suspend fun getAllActiveMemos(): List<Memo>

    suspend fun saveAllMemos(memos: List<Memo>)

    suspend fun importAll(tags: List<Tag>, memos: List<Memo>)

}
