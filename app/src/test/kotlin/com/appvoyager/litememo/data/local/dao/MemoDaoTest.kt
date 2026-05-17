package com.appvoyager.litememo.data.local.dao

import com.appvoyager.litememo.data.local.entity.MemoEntity
import com.appvoyager.litememo.data.local.entity.MemoTagRefEntity
import com.appvoyager.litememo.data.local.model.MemoWithTagRefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
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
            runTest { dao.upsertMemoWithTags(memo, tagRefs) }
        }

        // Assert
        assertEquals(emptyList<String>(), dao.calls)
    }

    @Test
    fun upsertMemoWithTagsReplacesTagRefsAfterWritingMemo() = runTest {
        // Arrange
        val dao = RecordingMemoDao()
        val memo = memoEntity(id = "memo-1")
        val tagRefs = listOf(
            MemoTagRefEntity(memoId = "memo-1", tagId = "tag-1", position = 0),
            MemoTagRefEntity(memoId = "memo-1", tagId = "tag-2", position = 1)
        )

        // Act
        dao.upsertMemoWithTags(memo, tagRefs)

        // Assert
        assertEquals(
            listOf(
                "upsertMemo:memo-1",
                "deleteTagRefsForMemo:memo-1",
                "insertTagRefs:memo-1:tag-1:0,memo-1:tag-2:1"
            ),
            dao.calls
        )
    }

    @Test
    fun upsertMemoWithTagsSkipsInsertWhenTagRefsAreEmpty() = runTest {
        // Arrange
        val dao = RecordingMemoDao()
        val memo = memoEntity(id = "memo-1")

        // Act
        dao.upsertMemoWithTags(memo, emptyList())

        // Assert
        assertEquals(
            listOf(
                "upsertMemo:memo-1",
                "deleteTagRefsForMemo:memo-1"
            ),
            dao.calls
        )
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

        val calls = mutableListOf<String>()

        override fun observeMemos(): Flow<List<MemoEntity>> = flowOf(emptyList())

        override fun observeMemosWithTagRefs(): Flow<List<MemoWithTagRefs>> = flowOf(emptyList())

        override fun observeMemosWithTagRefsCreatedBetween(
            fromMillis: Long,
            toMillis: Long
        ): Flow<List<MemoWithTagRefs>> = flowOf(emptyList())

        override fun observeMemoTagRefs(): Flow<List<MemoTagRefEntity>> = flowOf(emptyList())

        override suspend fun getMemoWithTagRefs(id: String): MemoWithTagRefs? = null

        override suspend fun upsertMemo(memo: MemoEntity) {
            calls += "upsertMemo:${memo.id}"
        }

        override suspend fun insertTagRefs(tagRefs: List<MemoTagRefEntity>) {
            val refs = tagRefs.joinToString(",") { "${it.memoId}:${it.tagId}:${it.position}" }
            calls += "insertTagRefs:$refs"
        }

        override suspend fun deleteTagRefsForMemo(memoId: String) {
            calls += "deleteTagRefsForMemo:$memoId"
        }

        override suspend fun deleteMemo(id: String) {
            calls += "deleteMemo:$id"
        }
    }

}
