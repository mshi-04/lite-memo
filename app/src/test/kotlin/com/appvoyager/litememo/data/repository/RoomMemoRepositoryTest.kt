package com.appvoyager.litememo.data.repository

import com.appvoyager.litememo.data.local.dao.MemoDao
import com.appvoyager.litememo.data.local.entity.MemoEntity
import com.appvoyager.litememo.data.local.entity.MemoTagRefEntity
import com.appvoyager.litememo.data.local.model.MemoWithTagRefs
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.SearchQuery
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
    fun observeActiveMemosReturnsDomainMemosFromDao() = runTest {
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
        val memos = repository.observeActiveMemos().first()

        // Assert
        assertEquals(listOf(MemoId("memo-1")), memos.map { it.id })
    }

    @Test
    fun observeActiveMemosBySearchQueryDelegatesEscapedLikePatternToDao() = runTest {
        // Arrange
        val dao = FakeMemoDao()
        val repository = RoomMemoRepository(dao)

        // Act
        repository.observeActiveMemosBySearchQuery(SearchQuery("100%_\\")).first()

        // Assert
        assertEquals("%100\\%\\_\\\\%", dao.observedSearchPattern)
    }

    @Test
    fun observeActiveMemosBySearchQueryReturnsDomainMemosFromDao() = runTest {
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
        val memos = repository.observeActiveMemosBySearchQuery(SearchQuery("title")).first()

        // Assert
        assertEquals(listOf(MemoId("memo-1")), memos.map { it.id })
    }

    @Test
    fun observeActiveMemosCreatedBetweenDelegatesTimestampValuesToDao() = runTest {
        // Arrange
        val dao = FakeMemoDao()
        val repository = RoomMemoRepository(dao)

        // Act
        repository.observeActiveMemosCreatedBetween(
            from = TimestampMillis(1_000L),
            to = TimestampMillis(2_000L)
        ).first()

        // Assert
        assertEquals(ObservedRange(fromMillis = 1_000L, toMillis = 2_000L), dao.observedRange)
    }

    @Test
    fun observeActiveMemosCreatedBetweenReturnsDomainMemosFromDao() = runTest {
        // Arrange
        val dao = FakeMemoDao(
            memosWithTagRefs = listOf(memoWithTagRefs(memoId = "memo-1"))
        )
        val repository = RoomMemoRepository(dao)

        // Act
        val memos = repository.observeActiveMemosCreatedBetween(
            from = TimestampMillis(500L),
            to = TimestampMillis(2_000L)
        ).first()

        // Assert
        assertEquals(listOf(MemoId("memo-1")), memos.map { it.id })
    }

    @Test
    fun observeActiveMemosCreatedBetweenUsesInclusiveStartAndExclusiveEnd() = runTest {
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
        val memos = repository.observeActiveMemosCreatedBetween(
            from = TimestampMillis(1_000L),
            to = TimestampMillis(2_000L)
        ).first()

        // Assert
        assertEquals(listOf(MemoId("memo-start")), memos.map { it.id })
    }

    @Test
    fun observeActiveMemosCreatedBetweenWithEqualRangeThrowsBeforeCallingDao() {
        // Arrange
        val dao = FakeMemoDao(failOnObserveMemosBetween = true)
        val repository = RoomMemoRepository(dao)

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            repository.observeActiveMemosCreatedBetween(
                from = TimestampMillis(1_000L),
                to = TimestampMillis(1_000L)
            )
        }
    }

    @Test
    fun observeActiveMemosCreatedBetweenWithDescendingRangeThrowsBeforeCallingDao() {
        // Arrange
        val dao = FakeMemoDao(failOnObserveMemosBetween = true)
        val repository = RoomMemoRepository(dao)

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            repository.observeActiveMemosCreatedBetween(
                from = TimestampMillis(2_000L),
                to = TimestampMillis(1_000L)
            )
        }
    }

    @Test
    fun getActiveMemoReturnsNullWhenDaoReturnsNull() = runTest {
        // Arrange
        val repository = RoomMemoRepository(FakeMemoDao())

        // Act
        val memo = repository.getActiveMemo(MemoId("missing"))

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
    fun observeTrashedMemosReturnsOnlyTrashedMemosOrderedByDeletedAtDescending() = runTest {
        // Arrange
        val dao = FakeMemoDao(
            memosWithTagRefs = listOf(
                memoWithTagRefs(memoId = "memo-active"),
                memoWithTagRefs(memoId = "memo-old", deletedAt = 1_000L),
                memoWithTagRefs(memoId = "memo-new", deletedAt = 2_000L)
            )
        )
        val repository = RoomMemoRepository(dao)

        // Act
        val memos = repository.observeTrashedMemos().first()

        // Assert
        assertEquals(listOf(MemoId("memo-new"), MemoId("memo-old")), memos.map { it.id })
    }

    @Test
    fun moveMemoToTrashDelegatesMemoIdAndDeletedAtValuesToDao() = runTest {
        // Arrange
        val dao = FakeMemoDao()
        val repository = RoomMemoRepository(dao)

        // Act
        repository.moveMemoToTrash(MemoId("memo-1"), TimestampMillis(2_000L))

        // Assert
        assertEquals(MovedToTrashRecord(memoId = "memo-1", deletedAt = 2_000L), dao.movedToTrash)
    }

    @Test
    fun restoreMemoFromTrashDelegatesMemoIdValueToDao() = runTest {
        // Arrange
        val dao = FakeMemoDao()
        val repository = RoomMemoRepository(dao)

        // Act
        repository.restoreMemoFromTrash(MemoId("memo-1"))

        // Assert
        assertEquals("memo-1", dao.restoredMemoId)
    }

    @Test
    fun deleteMemoPermanentlyDelegatesMemoIdValueToDao() = runTest {
        // Arrange
        val dao = FakeMemoDao()
        val repository = RoomMemoRepository(dao)

        // Act
        repository.deleteMemoPermanently(MemoId("memo-1"))

        // Assert
        assertEquals("memo-1", dao.permanentlyDeletedMemoId)
    }

    @Test
    fun deleteMemoPermanentlyThrowsWhenDaoDoesNotDeleteMemo() {
        // Arrange
        val dao = FakeMemoDao(deletedPermanentlyCount = 0)
        val repository = RoomMemoRepository(dao)

        // Act & Assert
        assertThrows(IllegalStateException::class.java) {
            runTest { repository.deleteMemoPermanently(MemoId("memo-1")) }
        }
    }

    @Test
    fun deleteTrashedMemosDeletedAtOrBeforeDelegatesCutoffValueToDao() = runTest {
        // Arrange
        val dao = FakeMemoDao()
        val repository = RoomMemoRepository(dao)

        // Act
        repository.deleteTrashedMemosDeletedAtOrBefore(TimestampMillis(2_000L))

        // Assert
        assertEquals(2_000L, dao.purgeCutoff)
    }

    private fun memoWithTagRefs(
        memoId: String,
        createdAt: Long = 1000L,
        updatedAt: Long = 1000L,
        tagRefs: List<MemoTagRefEntity> = emptyList(),
        deletedAt: Long? = null
    ) = MemoWithTagRefs(
        memo = MemoEntity(
            id = memoId,
            title = "Title",
            body = "Body",
            createdAt = createdAt,
            updatedAt = updatedAt,
            isFavorite = false,
            deletedAt = deletedAt
        ),
        tagRefs = tagRefs
    )

    private data class MovedToTrashRecord(val memoId: String, val deletedAt: Long)

    private data class ObservedRange(val fromMillis: Long, val toMillis: Long)

    private class FakeMemoDao(
        memosWithTagRefs: List<MemoWithTagRefs> = emptyList(),
        private val failOnObserveMemosBetween: Boolean = false,
        private val deletedPermanentlyCount: Int = 1
    ) : MemoDao {

        private val memosWithTagRefs = MutableStateFlow(memosWithTagRefs)
        var savedMemo: MemoEntity? = null
        var savedTagRefs: List<MemoTagRefEntity> = emptyList()
        var movedToTrash: MovedToTrashRecord? = null
        var restoredMemoId: String? = null
        var permanentlyDeletedMemoId: String? = null
        var purgeCutoff: Long? = null
        var observedRange: ObservedRange? = null
        var observedSearchPattern: String? = null

        override fun observeActiveMemosWithTagRefs(): Flow<List<MemoWithTagRefs>> = memosWithTagRefs

        override fun observeActiveMemosWithTagRefsBySearchPattern(
            pattern: String
        ): Flow<List<MemoWithTagRefs>> {
            observedSearchPattern = pattern
            return memosWithTagRefs
        }

        override fun observeActiveMemosWithTagRefsCreatedBetween(
            fromMillis: Long,
            toMillis: Long
        ): Flow<List<MemoWithTagRefs>> {
            if (failOnObserveMemosBetween) {
                fail<Nothing>("observeActiveMemosWithTagRefsCreatedBetween should not be called.")
            }
            observedRange = ObservedRange(fromMillis = fromMillis, toMillis = toMillis)
            return memosWithTagRefs.map { list ->
                list.filter { it.memo.createdAt >= fromMillis && it.memo.createdAt < toMillis }
            }
        }

        override suspend fun getActiveMemoWithTagRefs(id: String): MemoWithTagRefs? =
            memosWithTagRefs.value.firstOrNull { it.memo.id == id }

        override fun observeTrashedMemosWithTagRefs(): Flow<List<MemoWithTagRefs>> =
            memosWithTagRefs.map { list ->
                list
                    .filter { it.memo.deletedAt != null }
                    .sortedByDescending { it.memo.deletedAt }
            }

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

        override suspend fun moveMemoToTrash(id: String, deletedAt: Long): Int {
            movedToTrash = MovedToTrashRecord(memoId = id, deletedAt = deletedAt)
            return 1
        }

        override suspend fun restoreMemoFromTrash(id: String): Int {
            restoredMemoId = id
            return 1
        }

        override suspend fun deleteMemoPermanently(id: String): Int {
            permanentlyDeletedMemoId = id
            return deletedPermanentlyCount
        }

        override suspend fun deleteTrashedMemosDeletedAtOrBefore(cutoff: Long) {
            purgeCutoff = cutoff
        }
    }

}
