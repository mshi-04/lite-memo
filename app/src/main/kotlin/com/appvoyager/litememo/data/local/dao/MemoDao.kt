package com.appvoyager.litememo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.appvoyager.litememo.data.local.entity.MemoEntity
import com.appvoyager.litememo.data.local.entity.MemoTagRefEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {

    @Query("SELECT * FROM memos")
    fun observeMemos(): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memo_tag_refs")
    fun observeMemoTagRefs(): Flow<List<MemoTagRefEntity>>

    @Query("SELECT * FROM memos WHERE id = :id")
    suspend fun getMemo(id: String): MemoEntity?

    @Query("SELECT * FROM memo_tag_refs WHERE memoId = :memoId ORDER BY position ASC")
    suspend fun getTagRefsForMemo(memoId: String): List<MemoTagRefEntity>

    @Upsert
    suspend fun upsertMemo(memo: MemoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTagRefs(tagRefs: List<MemoTagRefEntity>)

    @Query("DELETE FROM memo_tag_refs WHERE memoId = :memoId")
    suspend fun deleteTagRefsForMemo(memoId: String)

    @Query("DELETE FROM memos WHERE id = :id")
    suspend fun deleteMemo(id: String)

    @Transaction
    suspend fun upsertMemoWithTags(memo: MemoEntity, tagRefs: List<MemoTagRefEntity>) {
        upsertMemo(memo)
        deleteTagRefsForMemo(memo.id)
        if (tagRefs.isNotEmpty()) {
            insertTagRefs(tagRefs)
        }
    }

}
