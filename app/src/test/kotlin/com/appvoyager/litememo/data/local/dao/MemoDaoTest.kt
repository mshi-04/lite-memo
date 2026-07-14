package com.appvoyager.litememo.data.local.dao

import com.appvoyager.litememo.data.local.entity.MemoEntity
import com.appvoyager.litememo.data.local.entity.MemoImageEntity
import com.appvoyager.litememo.data.local.entity.MemoTagRefEntity
import com.appvoyager.litememo.data.local.model.MemoWithRefs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class MemoDaoTest {

    @Test
    fun upsertMemoWithRefsThrowsBeforeWritingWhenTagRefsReferenceAnotherMemo() {
        // Arrange
        val dao = RecordingMemoDao(failOnWrite = true)
        val memo = memoEntity(id = "memo-1")
        val tagRefs = listOf(MemoTagRefEntity(memoId = "memo-2", tagId = "tag-1", position = 0))

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            runTest { dao.upsertMemoWithRefs(memo, tagRefs, emptyList()) }
        }
    }

    @Test
    fun upsertMemoWithRefsThrowsBeforeWritingWhenImageRefsReferenceAnotherMemo() {
        // Arrange
        val dao = RecordingMemoDao(failOnWrite = true)
        val memo = memoEntity(id = "memo-1")
        val imageRefs = listOf(
            MemoImageEntity(
                id = "image-1",
                memoId = "memo-2",
                fileName = "image-1.jpg",
                position = 0
            )
        )

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            runTest { dao.upsertMemoWithRefs(memo, emptyList(), imageRefs) }
        }
    }

    @Test
    fun upsertMemoWithRefsReplacesRefsAfterWritingMemo() = runTest {
        // Arrange
        val dao = RecordingMemoDao()
        val memo = memoEntity(id = "memo-1")
        val tagRefs = listOf(
            MemoTagRefEntity(memoId = "memo-1", tagId = "tag-1", position = 0),
            MemoTagRefEntity(memoId = "memo-1", tagId = "tag-2", position = 1)
        )
        val imageRefs = listOf(
            MemoImageEntity(
                id = "image-1",
                memoId = "memo-1",
                fileName = "image-1.jpg",
                position = 0
            )
        )

        // Act
        dao.upsertMemoWithRefs(memo, tagRefs, imageRefs)

        // Assert
        assertEquals(
            listOf(
                "upsertMemo:memo-1",
                "deleteTagRefsForMemo:memo-1",
                "insertTagRefs:memo-1:tag-1:0,memo-1:tag-2:1",
                "deleteImageRefsForMemo:memo-1",
                "insertImageRefs:image-1:memo-1:image-1.jpg:0"
            ),
            dao.calls
        )
    }

    @Test
    fun upsertMemoWithRefsSkipsInsertWhenRefsAreEmpty() = runTest {
        // Arrange
        val dao = RecordingMemoDao()
        val memo = memoEntity(id = "memo-1")

        // Act
        dao.upsertMemoWithRefs(memo, emptyList(), emptyList())

        // Assert
        assertEquals(
            listOf(
                "upsertMemo:memo-1",
                "deleteTagRefsForMemo:memo-1",
                "deleteImageRefsForMemo:memo-1"
            ),
            dao.calls
        )
    }

    @Test
    fun upsertAllMemosWithRefsCollectsImageFileNamesInBatches() = runTest {
        // Arrange
        val dao = RecordingMemoDao()
        val memos = List(901) { index -> memoEntity(id = "memo-$index") }

        // Act
        dao.upsertAllMemosWithRefsAndCollectRemovedFileNames(
            memos = memos,
            tagRefsByMemoId = emptyMap(),
            imageRefsByMemoId = emptyMap()
        )

        // Assert
        assertEquals(listOf(900, 1), dao.imageFileNameBatchSizes)
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
        val imageFileNameBatchSizes = mutableListOf<Int>()
        private val emptyMemoFlow = flowOf(emptyList<MemoWithRefs>())

        override fun observeActiveMemosWithRefs() = emptyMemoFlow

        override fun observeActiveMemosWithRefsBySearchPattern(pattern: String) = emptyMemoFlow

        override fun observeActiveMemosWithRefsCreatedBetween(fromMillis: Long, toMillis: Long) =
            emptyMemoFlow

        override suspend fun getActiveMemoWithRefs(id: String): MemoWithRefs? = null

        override fun observeTrashedMemosWithRefs() = emptyMemoFlow

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

        override suspend fun insertImageRefs(imageRefs: List<MemoImageEntity>) {
            if (failOnWrite) {
                fail<Nothing>("insertImageRefs should not be called.")
            }
            val refs = imageRefs.joinToString(",") {
                "${it.id}:${it.memoId}:${it.fileName}:${it.position}"
            }
            calls += "insertImageRefs:$refs"
        }

        override suspend fun deleteTagRefsForMemo(memoId: String) {
            if (failOnWrite) {
                fail<Nothing>("deleteTagRefsForMemo should not be called.")
            }
            calls += "deleteTagRefsForMemo:$memoId"
        }

        override suspend fun deleteImageRefsForMemo(memoId: String) {
            if (failOnWrite) {
                fail<Nothing>("deleteImageRefsForMemo should not be called.")
            }
            calls += "deleteImageRefsForMemo:$memoId"
        }

        override suspend fun getImageFileNamesForMemo(memoId: String): List<String> = emptyList()

        override suspend fun getImageFileNamesForMemos(memoIds: List<String>): List<String> {
            imageFileNameBatchSizes += memoIds.size
            return emptyList()
        }

        override suspend fun getImageFileNamesForTrashedMemosDeletedAtOrBefore(
            cutoff: Long
        ): List<String> = emptyList()

        override suspend fun moveMemoToTrash(id: String, deletedAt: Long): Int {
            if (failOnWrite) {
                fail<Nothing>("moveMemoToTrash should not be called.")
            }
            calls += "moveMemoToTrash:$id:$deletedAt"
            return 1
        }

        override suspend fun restoreMemoFromTrash(id: String): Int = 1

        override suspend fun deleteMemoPermanently(id: String): Int = 1

        override suspend fun discardMemo(id: String): Int = 1

        override suspend fun deleteTrashedMemosDeletedAtOrBefore(cutoff: Long) = Unit

        override suspend fun getAllActiveMemosWithRefs(): List<MemoWithRefs> = emptyList()
    }

}
