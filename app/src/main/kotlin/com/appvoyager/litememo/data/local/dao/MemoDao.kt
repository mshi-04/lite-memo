package com.appvoyager.litememo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.appvoyager.litememo.data.local.entity.MemoEntity
import com.appvoyager.litememo.data.local.entity.MemoTagRefEntity
import com.appvoyager.litememo.data.local.model.MemoWithTagRefs
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {

    @Transaction
    @Query("SELECT * FROM memos")
    fun observeMemosWithTagRefs(): Flow<List<MemoWithTagRefs>>

    @Transaction
    @Query("SELECT * FROM memos WHERE createdAt >= :fromMillis AND createdAt < :toMillis")
    fun observeMemosWithTagRefsCreatedBetween(
        fromMillis: Long,
        toMillis: Long
    ): Flow<List<MemoWithTagRefs>>

    @Transaction
    @Query("SELECT * FROM memos WHERE id = :id")
    suspend fun getMemoWithTagRefs(id: String): MemoWithTagRefs?

    @Upsert
    suspend fun upsertMemo(memo: MemoEntity)

    @Insert
    suspend fun insertTagRefs(tagRefs: List<MemoTagRefEntity>)

    @Query("DELETE FROM memo_tag_refs WHERE memoId = :memoId")
    suspend fun deleteTagRefsForMemo(memoId: String)

    @Query("DELETE FROM memos WHERE id = :id")
    suspend fun deleteMemo(id: String)

    @Transaction
    suspend fun upsertMemoWithTags(memo: MemoEntity, tagRefs: List<MemoTagRefEntity>) {
        require(tagRefs.all { it.memoId == memo.id }) {
            "All tagRefs must reference memoId=${memo.id}."
        }

        upsertMemo(memo)
        deleteTagRefsForMemo(memo.id)
        if (tagRefs.isNotEmpty()) {
            insertTagRefs(tagRefs)
        }
    }

}
