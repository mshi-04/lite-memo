package com.appvoyager.litememo.data.local.dao

import com.appvoyager.litememo.data.local.entity.MemoEntity
import com.appvoyager.litememo.data.local.entity.MemoTagRefEntity
import com.appvoyager.litememo.data.local.model.MemoWithTagRefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MemoDaoTest {

    @Test
    fun upsertMemoWithTagsThrowsBeforeWritingWhenTagRefsReferenceAnotherMemo() {
        // Arrange
        val dao = RecordingMemoDao()
        val memo = memoEntity(id = "memo-1")
        val tagRefs = listOf(MemoTagRefEntity(memoId = "memo-2", tagId = "tag-1", position = 0))

        // Act
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { dao.upsertMemoWithTags(memo, tagRefs) }
        }

        // Assert
        assertFalse(dao.wrote)
    }

    private fun memoEntity(id: String) = MemoEntity(
        id = id,
        title = "Title",
        body = "Body",
        createdAt = 1000L,
        updatedAt = 1000L,
        isImportant = false
    )

    private class RecordingMemoDao : MemoDao {

        var wrote = false

        override fun observeMemos(): Flow<List<MemoEntity>> = flowOf(emptyList())

        override fun observeMemosWithTagRefs(): Flow<List<MemoWithTagRefs>> = flowOf(emptyList())

        override fun observeMemosWithTagRefsBetween(
            fromMillis: Long,
            toMillis: Long
        ): Flow<List<MemoWithTagRefs>> = flowOf(emptyList())

        override fun observeMemoTagRefs(): Flow<List<MemoTagRefEntity>> = flowOf(emptyList())

        override suspend fun getMemoWithTagRefs(id: String): MemoWithTagRefs? = null

        override suspend fun upsertMemo(memo: MemoEntity) {
            wrote = true
        }

        override suspend fun insertTagRefs(tagRefs: List<MemoTagRefEntity>) {
            wrote = true
        }

        override suspend fun deleteTagRefsForMemo(memoId: String) {
            wrote = true
        }

        override suspend fun deleteMemo(id: String) {
            wrote = true
        }
    }

}
