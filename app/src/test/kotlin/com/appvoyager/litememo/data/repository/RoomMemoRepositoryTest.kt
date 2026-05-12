package com.appvoyager.litememo.data.repository

import com.appvoyager.litememo.data.local.dao.MemoDao
import com.appvoyager.litememo.data.local.entity.MemoEntity
import com.appvoyager.litememo.data.local.entity.MemoTagRefEntity
import com.appvoyager.litememo.data.local.model.MemoWithTagRefs
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.TagId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RoomMemoRepositoryTest {

    @Test
    fun observeMemosReturnsDomainMemosFromDao() = runBlocking {
        // Arrange
        val dao = FakeMemoDao(
            memosWithTagRefs = listOf(
                memoWithTagRefs(
                    memoId = "memo-1",
                    tagRefs = listOf(
                        MemoTagRefEntity(memoId = "memo-1", tagId = "tag-1", position = 0)
                    )
                )
            )
        )
        val repository = RoomMemoRepository(dao)

        // Act
        val memos = repository.observeMemos().first()

        // Assert
        assertEquals(listOf(MemoId("memo-1")), memos.map { it.id })
    }

    @Test
    fun getMemoReturnsNullWhenDaoReturnsNull() = runBlocking {
        // Arrange
        val repository = RoomMemoRepository(FakeMemoDao())

        // Act
        val memo = repository.getMemo(MemoId("missing"))

        // Assert
        assertNull(memo)
    }

    @Test
    fun saveMemoWritesMemoEntityToDao() = runBlocking {
        // Arrange
        val dao = FakeMemoDao()
        val repository = RoomMemoRepository(dao)

        // Act
        repository.saveMemo(memoFixture(id = "memo-1", title = "Title"))

        // Assert
        assertEquals("memo-1", dao.savedMemo?.id)
    }

    @Test
    fun saveMemoWritesTagRefsToDao() = runBlocking {
        // Arrange
        val dao = FakeMemoDao()
        val repository = RoomMemoRepository(dao)

        // Act
        repository.saveMemo(
            memoFixture(
                id = "memo-1",
                tagIds = listOf(TagId("tag-1"), TagId("tag-2"))
            )
        )

        // Assert
        assertEquals(
            listOf(
                MemoTagRefEntity(memoId = "memo-1", tagId = "tag-1", position = 0),
                MemoTagRefEntity(memoId = "memo-1", tagId = "tag-2", position = 1)
            ),
            dao.savedTagRefs
        )
    }

    @Test
    fun deleteMemoDelegatesMemoIdValueToDao() = runBlocking {
        // Arrange
        val dao = FakeMemoDao()
        val repository = RoomMemoRepository(dao)

        // Act
        repository.deleteMemo(MemoId("memo-1"))

        // Assert
        assertEquals("memo-1", dao.deletedMemoId)
    }

    private fun memoWithTagRefs(memoId: String, tagRefs: List<MemoTagRefEntity> = emptyList()) =
        MemoWithTagRefs(
            memo = MemoEntity(
                id = memoId,
                title = "Title",
                body = "Body",
                createdAt = 1000L,
                updatedAt = 1000L,
                isImportant = false
            ),
            tagRefs = tagRefs
        )

    private class FakeMemoDao(memosWithTagRefs: List<MemoWithTagRefs> = emptyList()) : MemoDao {

        private val memosWithTagRefs = MutableStateFlow(memosWithTagRefs)
        var savedMemo: MemoEntity? = null
        var savedTagRefs: List<MemoTagRefEntity> = emptyList()
        var deletedMemoId: String? = null

        override fun observeMemos(): Flow<List<MemoEntity>> =
            MutableStateFlow(memosWithTagRefs.value.map { it.memo })

        override fun observeMemosWithTagRefs(): Flow<List<MemoWithTagRefs>> = memosWithTagRefs

        override fun observeMemoTagRefs(): Flow<List<MemoTagRefEntity>> =
            MutableStateFlow(memosWithTagRefs.value.flatMap { it.tagRefs })

        override suspend fun getMemoWithTagRefs(id: String): MemoWithTagRefs? =
            memosWithTagRefs.value.firstOrNull { it.memo.id == id }

        override suspend fun upsertMemo(memo: MemoEntity) {
            savedMemo = memo
        }

        override suspend fun insertTagRefs(tagRefs: List<MemoTagRefEntity>) {
            savedTagRefs = tagRefs
        }

        override suspend fun deleteTagRefsForMemo(memoId: String) {
            savedTagRefs = emptyList()
        }

        override suspend fun deleteMemo(id: String) {
            deletedMemoId = id
        }
    }

}
