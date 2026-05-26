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
    @Query("SELECT * FROM memos WHERE deletedAt IS NULL")
    fun observeActiveMemosWithTagRefs(): Flow<List<MemoWithTagRefs>>

    @Transaction
    @Query(
        """
        SELECT * FROM memos
        WHERE deletedAt IS NULL
            AND (
                title LIKE :pattern ESCAPE '\'
                OR body LIKE :pattern ESCAPE '\'
            )
        """
    )
    fun observeActiveMemosWithTagRefsBySearchPattern(pattern: String): Flow<List<MemoWithTagRefs>>

    @Transaction
    @Query(
        """
        SELECT * FROM memos
        WHERE deletedAt IS NULL
            AND createdAt >= :fromMillis
            AND createdAt < :toMillis
        """
    )
    fun observeActiveMemosWithTagRefsCreatedBetween(
        fromMillis: Long,
        toMillis: Long
    ): Flow<List<MemoWithTagRefs>>

    @Transaction
    @Query("SELECT * FROM memos WHERE id = :id AND deletedAt IS NULL")
    suspend fun getActiveMemoWithTagRefs(id: String): MemoWithTagRefs?

    @Transaction
    @Query("SELECT * FROM memos WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrashedMemosWithTagRefs(): Flow<List<MemoWithTagRefs>>

    @Upsert
    suspend fun upsertMemo(memo: MemoEntity)

    @Insert
    suspend fun insertTagRefs(tagRefs: List<MemoTagRefEntity>)

    @Query("DELETE FROM memo_tag_refs WHERE memoId = :memoId")
    suspend fun deleteTagRefsForMemo(memoId: String)

    @Query("UPDATE memos SET deletedAt = :deletedAt WHERE id = :id AND deletedAt IS NULL")
    suspend fun moveMemoToTrash(id: String, deletedAt: Long): Int

    @Query("UPDATE memos SET deletedAt = NULL WHERE id = :id AND deletedAt IS NOT NULL")
    suspend fun restoreMemoFromTrash(id: String): Int

    @Query("DELETE FROM memos WHERE id = :id AND deletedAt IS NOT NULL")
    suspend fun deleteMemoPermanently(id: String): Int

    @Query("DELETE FROM memos WHERE deletedAt IS NOT NULL AND deletedAt <= :cutoff")
    suspend fun deleteTrashedMemosDeletedAtOrBefore(cutoff: Long)

    @Transaction
    @Query("SELECT * FROM memos WHERE deletedAt IS NULL")
    suspend fun getAllActiveMemosWithTagRefs(): List<MemoWithTagRefs>

    @Transaction
    suspend fun upsertAllMemosWithTags(
        memos: List<MemoEntity>,
        tagRefsByMemoId: Map<String, List<MemoTagRefEntity>>
    ) {
        memos.forEach { memo ->
            val refs = tagRefsByMemoId[memo.id] ?: emptyList()
            upsertMemoWithTags(memo, refs)
        }
    }

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
