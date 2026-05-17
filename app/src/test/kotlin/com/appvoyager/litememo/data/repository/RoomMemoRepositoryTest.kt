package com.appvoyager.litememo.data.repository

import com.appvoyager.litememo.data.local.dao.MemoDao
import com.appvoyager.litememo.data.local.entity.MemoEntity
import com.appvoyager.litememo.data.local.entity.MemoTagRefEntity
import com.appvoyager.litememo.data.local.model.MemoWithTagRefs
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class RoomMemoRepositoryTest {

    @Test
    fun observeMemosReturnsDomainMemosFromDao() = runTest {
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
    fun observeMemosCreatedBetweenDelegatesTimestampValuesToDao() = runTest {
        // Arrange
        val dao = FakeMemoDao()
        val repository = RoomMemoRepository(dao)

        // Act
        repository.observeMemosCreatedBetween(
            from = TimestampMillis(1_000L),
            to = TimestampMillis(2_000L)
        ).first()

        // Assert
        assertEquals(1_000L to 2_000L, dao.observedRange)
    }

    @Test
    fun observeMemosCreatedBetweenReturnsDomainMemosFromDao() = runTest {
        // Arrange
        val dao = FakeMemoDao(
            memosWithTagRefs = listOf(memoWithTagRefs(memoId = "memo-1"))
        )
        val repository = RoomMemoRepository(dao)

        // Act
        val memos = repository.observeMemosCreatedBetween(
            from = TimestampMillis(500L),
            to = TimestampMillis(2_000L)
        ).first()

        // Assert
        assertEquals(listOf(MemoId("memo-1")), memos.map { it.id })
    }

    @Test
    fun observeMemosCreatedBetweenUsesInclusiveStartAndExclusiveEnd() = runTest {
        // Arrange
        val dao = FakeMemoDao(
            memosWithTagRefs = listOf(
                memoWithTagRefs(memoId = "memo-before", createdAt = 999L),
                memoWithTagRefs(memoId = "memo-start", createdAt = 1000L),
                memoWithTagRefs(memoId = "memo-end", createdAt = 2000L),
                memoWithTagRefs(memoId = "memo-after", createdAt = 2001L)
            )
        )
        val repository = RoomMemoRepository(dao)

        // Act
        val memos = repository.observeMemosCreatedBetween(
            from = TimestampMillis(1_000L),
            to = TimestampMillis(2_000L)
        ).first()

        // Assert
        assertEquals(listOf(MemoId("memo-start")), memos.map { it.id })
    }

    @Test
    fun observeMemosCreatedBetweenWithEqualRangeThrowsBeforeCallingDao() {
        // Arrange
        val dao = FakeMemoDao(failOnObserveMemosBetween = true)
        val repository = RoomMemoRepository(dao)

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            repository.observeMemosCreatedBetween(
                from = TimestampMillis(1_000L),
                to = TimestampMillis(1_000L)
            )
        }
    }

    @Test
    fun observeMemosCreatedBetweenWithDescendingRangeThrowsBeforeCallingDao() {
        // Arrange
        val dao = FakeMemoDao(failOnObserveMemosBetween = true)
        val repository = RoomMemoRepository(dao)

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            repository.observeMemosCreatedBetween(
                from = TimestampMillis(2_000L),
                to = TimestampMillis(1_000L)
            )
        }
    }

    @Test
    fun getMemoReturnsNullWhenDaoReturnsNull() = runTest {
        // Arrange
        val repository = RoomMemoRepository(FakeMemoDao())

        // Act
        val memo = repository.getMemo(MemoId("missing"))

        // Assert
        assertNull(memo)
    }

    @Test
    fun saveMemoWritesMemoEntityToDao() = runTest {
        // Arrange
        val dao = FakeMemoDao()
        val repository = RoomMemoRepository(dao)

        // Act
        repository.saveMemo(memoFixture(id = "memo-1", title = "Title"))

        // Assert
        assertEquals("memo-1", dao.savedMemo?.id)
    }

    @Test
    fun saveMemoWritesTagRefsToDao() = runTest {
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
    fun deleteMemoDelegatesMemoIdValueToDao() = runTest {
        // Arrange
        val dao = FakeMemoDao()
        val repository = RoomMemoRepository(dao)

        // Act
        repository.deleteMemo(MemoId("memo-1"))

        // Assert
        assertEquals("memo-1", dao.deletedMemoId)
    }

    private fun memoWithTagRefs(
        memoId: String,
        createdAt: Long = 1000L,
        updatedAt: Long = 1000L,
        tagRefs: List<MemoTagRefEntity> = emptyList()
    ) = MemoWithTagRefs(
        memo = MemoEntity(
            id = memoId,
            title = "Title",
            body = "Body",
            createdAt = createdAt,
            updatedAt = updatedAt,
            isImportant = false
        ),
        tagRefs = tagRefs
    )

    private class FakeMemoDao(
        memosWithTagRefs: List<MemoWithTagRefs> = emptyList(),
        private val failOnObserveMemosBetween: Boolean = false
    ) : MemoDao {

        private val memosWithTagRefs = MutableStateFlow(memosWithTagRefs)
        var savedMemo: MemoEntity? = null
        var savedTagRefs: List<MemoTagRefEntity> = emptyList()
        var deletedMemoId: String? = null
        var observedRange: Pair<Long, Long>? = null

        override fun observeMemosWithTagRefs(): Flow<List<MemoWithTagRefs>> = memosWithTagRefs

        override fun observeMemosWithTagRefsCreatedBetween(
            fromMillis: Long,
            toMillis: Long
        ): Flow<List<MemoWithTagRefs>> {
            if (failOnObserveMemosBetween) {
                fail<Nothing>("observeMemosWithTagRefsCreatedBetween should not be called.")
            }
            observedRange = fromMillis to toMillis
            return memosWithTagRefs.map { list ->
                list.filter { it.memo.createdAt >= fromMillis && it.memo.createdAt < toMillis }
            }
        }

        override suspend fun getMemoWithTagRefs(id: String): MemoWithTagRefs? =
            memosWithTagRefs.value.firstOrNull { it.memo.id == id }

        override suspend fun upsertMemoWithTags(memo: MemoEntity, tagRefs: List<MemoTagRefEntity>) {
            savedMemo = memo
            savedTagRefs = tagRefs
        }

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
