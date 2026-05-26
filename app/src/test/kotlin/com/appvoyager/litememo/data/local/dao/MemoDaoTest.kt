package com.appvoyager.litememo.data.local.dao

import com.appvoyager.litememo.data.local.entity.MemoEntity
import com.appvoyager.litememo.data.local.entity.MemoTagRefEntity
import com.appvoyager.litememo.data.local.model.MemoWithTagRefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class MemoDaoTest {

    @Test
    fun upsertMemoWithTagsThrowsBeforeWritingWhenTagRefsReferenceAnotherMemo() {
        // Arrange
        val dao = RecordingMemoDao(failOnWrite = true)
        val memo = memoEntity(id = "memo-1")
        val tagRefs = listOf(MemoTagRefEntity(memoId = "memo-2", tagId = "tag-1", position = 0))

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            runTest { dao.upsertMemoWithTags(memo, tagRefs) }
        }
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
        isFavorite = false,
        deletedAt = null
    )

    private class RecordingMemoDao(private val failOnWrite: Boolean = false) : MemoDao {

        val calls = mutableListOf<String>()

        override fun observeActiveMemosWithTagRefs(): Flow<List<MemoWithTagRefs>> =
            flowOf(emptyList())

        override fun observeActiveMemosWithTagRefsBySearchPattern(
            pattern: String
        ): Flow<List<MemoWithTagRefs>> = flowOf(emptyList())

        override fun observeActiveMemosWithTagRefsCreatedBetween(
            fromMillis: Long,
            toMillis: Long
        ): Flow<List<MemoWithTagRefs>> = flowOf(emptyList())

        override suspend fun getActiveMemoWithTagRefs(id: String): MemoWithTagRefs? = null

        override fun observeTrashedMemosWithTagRefs(): Flow<List<MemoWithTagRefs>> =
            flowOf(emptyList())

        override suspend fun upsertMemo(memo: MemoEntity) {
            if (failOnWrite) {
                fail<Nothing>("upsertMemo should not be called.")
            }
            calls += "upsertMemo:${memo.id}"
        }

        override suspend fun insertTagRefs(tagRefs: List<MemoTagRefEntity>) {
            if (failOnWrite) {
                fail<Nothing>("insertTagRefs should not be called.")
            }
            val refs = tagRefs.joinToString(",") { "${it.memoId}:${it.tagId}:${it.position}" }
            calls += "insertTagRefs:$refs"
        }

        override suspend fun deleteTagRefsForMemo(memoId: String) {
            if (failOnWrite) {
                fail<Nothing>("deleteTagRefsForMemo should not be called.")
            }
            calls += "deleteTagRefsForMemo:$memoId"
        }

        override suspend fun moveMemoToTrash(id: String, deletedAt: Long): Int {
            if (failOnWrite) {
                fail<Nothing>("moveMemoToTrash should not be called.")
            }
            calls += "moveMemoToTrash:$id:$deletedAt"
            return 1
        }

        override suspend fun restoreMemoFromTrash(id: String): Int = 1

        override suspend fun deleteMemoPermanently(id: String): Int = 1

        override suspend fun deleteTrashedMemosDeletedAtOrBefore(cutoff: Long) = Unit

        override suspend fun getAllActiveMemosWithTagRefs(): List<MemoWithTagRefs> = emptyList()

        override suspend fun upsertAllMemosWithTags(
            memos: List<MemoEntity>,
            tagRefsByMemoId: Map<String, List<MemoTagRefEntity>>
        ) {
            if (failOnWrite) {
                fail<Nothing>("upsertAllMemosWithTags should not be called.")
            }
            memos.forEach { memo ->
                calls += "upsertMemo:${memo.id}"
                calls += "deleteTagRefsForMemo:${memo.id}"
                val refs = tagRefsByMemoId[memo.id].orEmpty()
                if (refs.isNotEmpty()) {
                    val joined = refs.joinToString(",") { "${it.memoId}:${it.tagId}:${it.position}" }
                    calls += "insertTagRefs:$joined"
                }
            }
        }
    }

}
