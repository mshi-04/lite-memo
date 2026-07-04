package com.appvoyager.litememo.data.repository

import androidx.room.InvalidationTracker
import com.appvoyager.litememo.data.local.LiteMemoDatabase
import com.appvoyager.litememo.data.local.dao.MemoDao
import com.appvoyager.litememo.data.local.dao.TagDao
import com.appvoyager.litememo.data.local.entity.MemoEntity
import com.appvoyager.litememo.data.local.entity.MemoTagRefEntity
import com.appvoyager.litememo.data.local.entity.TagEntity
import com.appvoyager.litememo.data.local.model.MemoWithTagRefs
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.SearchQuery
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.tagFixture
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
        val repository = createRepository(dao)

        // Act
        val memos = repository.observeActiveMemos().first()

        // Assert
        assertEquals(listOf(MemoId("memo-1")), memos.map { it.id })
    }

    @Test
    fun observeActiveMemosBySearchQueryDelegatesEscapedLikePatternToDao() = runTest {
        // Arrange
        val dao = FakeMemoDao()
        val repository = createRepository(dao)

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
        val repository = createRepository(dao)

        // Act
        val memos = repository.observeActiveMemosBySearchQuery(SearchQuery("title")).first()

        // Assert
        assertEquals(listOf(MemoId("memo-1")), memos.map { it.id })
    }

    @Test
    fun observeActiveMemosCreatedBetweenDelegatesTimestampValuesToDao() = runTest {
        // Arrange
        val dao = FakeMemoDao()
        val repository = createRepository(dao)

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
        val repository = createRepository(dao)

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
        val repository = createRepository(dao)

        // Act
        val memos = repository.observeActiveMemosCreatedBetween(
            from = TimestampMillis(1_000L),
            to = TimestampMillis(2_000L)
        ).first()

        // Assert
        assertEquals(listOf(MemoId("memo-start")), memos.map { it.id })
    }

    @Test
    fun observeActiveMemosCreatedBetweenWithEqualRangeReturnsEmpty() = runTest {
        // Arrange
        val dao = FakeMemoDao(
            memosWithTagRefs = listOf(memoWithTagRefs(memoId = "memo-1", createdAt = 1_000L))
        )
        val repository = createRepository(dao)

        // Act
        val memos = repository.observeActiveMemosCreatedBetween(
            from = TimestampMillis(1_000L),
            to = TimestampMillis(1_000L)
        ).first()

        // Assert
        assertEquals(emptyList<MemoId>(), memos.map { it.id })
    }

    @Test
    fun observeActiveMemosCreatedBetweenWithDescendingRangeThrowsBeforeCallingDao() {
        // Arrange
        val dao = FakeMemoDao(failOnObserveMemosBetween = true)
        val repository = createRepository(dao)

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
        val repository = createRepository(FakeMemoDao())

        // Act
        val memo = repository.getActiveMemo(MemoId("missing"))

        // Assert
        assertNull(memo)
    }

    @Test
    fun saveMemoWritesMemoEntityToDao() = runTest {
        // Arrange
        val dao = FakeMemoDao()
        val repository = createRepository(dao)

        // Act
        repository.saveMemo(memoFixture(id = "memo-1", title = "Title"))

        // Assert
        assertEquals("memo-1", dao.savedMemo?.id)
    }

    @Test
    fun saveMemoWritesTagRefsToDao() = runTest {
        // Arrange
        val dao = FakeMemoDao()
        val repository = createRepository(dao)

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
        val repository = createRepository(dao)

        // Act
        val memos = repository.observeTrashedMemos().first()

        // Assert
        assertEquals(listOf(MemoId("memo-new"), MemoId("memo-old")), memos.map { it.id })
    }

    @Test
    fun moveMemoToTrashDelegatesMemoIdAndDeletedAtValuesToDao() = runTest {
        // Arrange
        val dao = FakeMemoDao()
        val repository = createRepository(dao)

        // Act
        repository.moveMemoToTrash(MemoId("memo-1"), TimestampMillis(2_000L))

        // Assert
        assertEquals(MovedToTrashRecord(memoId = "memo-1", deletedAt = 2_000L), dao.movedToTrash)
    }

    @Test
    fun restoreMemoFromTrashDelegatesMemoIdValueToDao() = runTest {
        // Arrange
        val dao = FakeMemoDao()
        val repository = createRepository(dao)

        // Act
        repository.restoreMemoFromTrash(MemoId("memo-1"))

        // Assert
        assertEquals("memo-1", dao.restoredMemoId)
    }

    @Test
    fun moveMemoToTrashThrowsWhenDaoDoesNotMoveMemo() {
        // Arrange
        val dao = FakeMemoDao(movedToTrashCount = 0)
        val repository = createRepository(dao)

        // Act & Assert
        // Error/Repository: DAO zero affected rows are exposed as an illegal state.
        assertThrows(IllegalStateException::class.java) {
            runTest { repository.moveMemoToTrash(MemoId("memo-1"), TimestampMillis(2_000L)) }
        }
    }

    @Test
    fun restoreMemoFromTrashThrowsWhenDaoDoesNotRestoreMemo() {
        // Arrange
        val dao = FakeMemoDao(restoredCount = 0)
        val repository = createRepository(dao)

        // Act & Assert
        // Error/Repository: DAO zero affected rows are exposed as an illegal state.
        assertThrows(IllegalStateException::class.java) {
            runTest { repository.restoreMemoFromTrash(MemoId("memo-1")) }
        }
    }

    @Test
    fun deleteMemoPermanentlyDelegatesMemoIdValueToDao() = runTest {
        // Arrange
        val dao = FakeMemoDao()
        val repository = createRepository(dao)

        // Act
        repository.deleteMemoPermanently(MemoId("memo-1"))

        // Assert
        assertEquals("memo-1", dao.permanentlyDeletedMemoId)
    }

    @Test
    fun deleteMemoPermanentlyThrowsWhenDaoDoesNotDeleteMemo() {
        // Arrange
        val dao = FakeMemoDao(deletedPermanentlyCount = 0)
        val repository = createRepository(dao)

        // Act & Assert
        assertThrows(IllegalStateException::class.java) {
            runTest { repository.deleteMemoPermanently(MemoId("memo-1")) }
        }
    }

    @Test
    fun discardMemoDelegatesMemoIdValueToDao() = runTest {
        // Arrange
        val dao = FakeMemoDao()
        val repository = createRepository(dao)

        // Act
        // Interaction: discard uses the unconditional DAO delete path.
        repository.discardMemo(MemoId("memo-1"))

        // Assert
        assertEquals("memo-1", dao.discardedMemoId)
    }

    @Test
    fun discardMemoDoesNotThrowWhenDaoDoesNotDeleteMemo() = runTest {
        // Arrange
        val dao = FakeMemoDao().apply { discardedCount = 0 }
        val repository = createRepository(dao)

        // Act
        // Boundary: missing abandoned rows are treated as no-op.
        val error = runCatching { repository.discardMemo(MemoId("missing")) }.exceptionOrNull()

        // Assert
        assertNull(error)
    }

    @Test
    fun deleteTrashedMemosDeletedAtOrBeforeDelegatesCutoffValueToDao() = runTest {
        // Arrange
        val dao = FakeMemoDao()
        val repository = createRepository(dao)

        // Act
        repository.deleteTrashedMemosDeletedAtOrBefore(TimestampMillis(2_000L))

        // Assert
        assertEquals(2_000L, dao.purgeCutoff)
    }

    @Test
    fun executeImportWritesTagEntitiesToDao() = runTest {
        // Arrange
        val memoDao = FakeMemoDao()
        val tagDao = FakeTagDao()
        val repository = createRepositoryForImport(memoDao, tagDao)
        val tag = tagFixture(id = "t1", name = "Tag1")

        // Act
        repository.executeImport(tags = listOf(tag), memos = emptyList())

        // Assert
        assertEquals(1, tagDao.upsertedTags.size)
        assertEquals("t1", tagDao.upsertedTags[0].id)
    }

    @Test
    fun executeImportWritesMemoEntitiesWithCorrectTagRefsToDao() = runTest {
        // Arrange
        val memoDao = FakeMemoDao()
        val tagDao = FakeTagDao()
        val repository = createRepositoryForImport(memoDao, tagDao)
        val memo = memoFixture(id = "m1", tagIds = listOf(TagId("t1"), TagId("t2")))

        // Act
        repository.executeImport(tags = emptyList(), memos = listOf(memo))

        // Assert
        assertEquals(1, memoDao.savedMemoBatches.size)
        assertEquals("m1", memoDao.savedMemoBatches[0].memo.id)
        assertEquals(
            listOf("t1", "t2"),
            memoDao.savedMemoBatches[0].tagRefs.map { it.tagId }
        )
    }

    @Test
    fun executeImportThrowsWhenDuplicateMemoIdsProvided() {
        // Arrange
        val repository = createRepositoryForImport(FakeMemoDao(), FakeTagDao())

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            runTest {
                repository.executeImport(
                    tags = emptyList(),
                    memos = listOf(memoFixture(id = "m1"), memoFixture(id = "m1"))
                )
            }
        }
    }

    @Test
    fun interactionExecuteImportDoesNotWriteWhenDuplicateMemoIdsProvided() = runTest {
        // Arrange
        val memoDao = FakeMemoDao()
        val tagDao = FakeTagDao()
        val repository = createRepositoryForImport(memoDao, tagDao)

        // Act
        // Interaction/Error: duplicate memo ids are rejected before any DAO write.
        runCatching {
            repository.executeImport(
                tags = listOf(tagFixture(id = "t1")),
                memos = listOf(memoFixture(id = "m1"), memoFixture(id = "m1"))
            )
        }

        // Assert
        assertEquals(NoImportWritesSnapshot(), importWritesSnapshot(tagDao, memoDao))
    }

    @Test
    fun executeImportThrowsWhenDuplicateTagIdsProvided() {
        // Arrange
        val repository = createRepositoryForImport(FakeMemoDao(), FakeTagDao())

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            runTest {
                repository.executeImport(
                    tags = listOf(tagFixture(id = "t1"), tagFixture(id = "t1")),
                    memos = emptyList()
                )
            }
        }
    }

    @Test
    fun interactionExecuteImportDoesNotWriteWhenDuplicateTagIdsProvided() = runTest {
        // Arrange
        val memoDao = FakeMemoDao()
        val tagDao = FakeTagDao()
        val repository = createRepositoryForImport(memoDao, tagDao)

        // Act
        // Interaction/Error: duplicate tag ids are rejected before any DAO write.
        runCatching {
            repository.executeImport(
                tags = listOf(tagFixture(id = "t1"), tagFixture(id = "t1")),
                memos = listOf(memoFixture(id = "m1"))
            )
        }

        // Assert
        assertEquals(NoImportWritesSnapshot(), importWritesSnapshot(tagDao, memoDao))
    }

    @Test
    fun executeImportCallsTagDaoBeforeMemoDao() = runTest {
        // Arrange
        val callOrder = mutableListOf<String>()
        val memoDao = FakeMemoDao(onUpsertAllMemosWithTags = { callOrder += "memos" })
        val tagDao = FakeTagDao(onUpsertAllTags = { callOrder += "tags" })
        val repository = createRepositoryForImport(memoDao, tagDao)
        val tag = tagFixture(id = "t1")
        val memo = memoFixture(id = "m1", tagIds = listOf(TagId("t1")))

        // Act
        repository.executeImport(tags = listOf(tag), memos = listOf(memo))

        // Assert
        assertEquals(listOf("tags", "memos"), callOrder)
    }

    private fun createRepositoryForImport(
        memoDao: FakeMemoDao,
        tagDao: FakeTagDao
    ): RoomMemoRepository {
        val dummyDatabase = object : LiteMemoDatabase() {
            override fun memoDao(): MemoDao = memoDao
            override fun tagDao(): TagDao = tagDao
            override fun createInvalidationTracker(): InvalidationTracker =
                throw UnsupportedOperationException()
            override fun clearAllTables() = throw UnsupportedOperationException()
        }
        return RoomMemoRepository(memoDao, tagDao, dummyDatabase)
    }

    private fun createRepository(dao: FakeMemoDao): RoomMemoRepository {
        // database is only used by importAll(), which is not tested in this unit test.
        val dummyDatabase = object : LiteMemoDatabase() {
            override fun memoDao(): MemoDao = dao
            override fun tagDao(): TagDao = throw UnsupportedOperationException()
            override fun createInvalidationTracker(): InvalidationTracker =
                throw UnsupportedOperationException()
            override fun clearAllTables() = throw UnsupportedOperationException()
        }
        return RoomMemoRepository(dao, FakeTagDao(), dummyDatabase)
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

    private data class SavedMemoBatch(val memo: MemoEntity, val tagRefs: List<MemoTagRefEntity>)

    private data class NoImportWritesSnapshot(
        val upsertedTagCount: Int = 0,
        val savedMemoBatchCount: Int = 0
    )

    private fun importWritesSnapshot(tagDao: FakeTagDao, memoDao: FakeMemoDao) =
        NoImportWritesSnapshot(
            upsertedTagCount = tagDao.upsertedTags.size,
            savedMemoBatchCount = memoDao.savedMemoBatches.size
        )

    private class FakeMemoDao(
        memosWithTagRefs: List<MemoWithTagRefs> = emptyList(),
        private val failOnObserveMemosBetween: Boolean = false,
        private val movedToTrashCount: Int = 1,
        private val restoredCount: Int = 1,
        private val deletedPermanentlyCount: Int = 1,
        private val onUpsertAllMemosWithTags: (() -> Unit)? = null
    ) : MemoDao {

        private val memosWithTagRefs = MutableStateFlow(memosWithTagRefs)
        var savedMemo: MemoEntity? = null
        var savedTagRefs: List<MemoTagRefEntity> = emptyList()
        val savedMemoBatches = mutableListOf<SavedMemoBatch>()
        var movedToTrash: MovedToTrashRecord? = null
        var restoredMemoId: String? = null
        var permanentlyDeletedMemoId: String? = null
        var discardedMemoId: String? = null
        var discardedCount: Int = 1
        var purgeCutoff: Long? = null
        var observedRange: ObservedRange? = null
        var observedSearchPattern: String? = null

        override fun observeActiveMemosWithTagRefs(): Flow<List<MemoWithTagRefs>> =
            memosWithTagRefs.map { list -> list.filter { it.memo.deletedAt == null } }

        override fun observeActiveMemosWithTagRefsBySearchPattern(
            pattern: String
        ): Flow<List<MemoWithTagRefs>> {
            observedSearchPattern = pattern
            return memosWithTagRefs.map { list -> list.filter { it.memo.deletedAt == null } }
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
                list.filter {
                    it.memo.deletedAt == null &&
                        it.memo.createdAt >= fromMillis &&
                        it.memo.createdAt < toMillis
                }
            }
        }

        override suspend fun getActiveMemoWithTagRefs(id: String): MemoWithTagRefs? =
            memosWithTagRefs.value.firstOrNull { it.memo.id == id && it.memo.deletedAt == null }

        override fun observeTrashedMemosWithTagRefs(): Flow<List<MemoWithTagRefs>> =
            memosWithTagRefs.map { list ->
                list
                    .filter { it.memo.deletedAt != null }
                    .sortedByDescending { it.memo.deletedAt }
            }

        override suspend fun upsertMemoWithTags(memo: MemoEntity, tagRefs: List<MemoTagRefEntity>) {
            savedMemo = memo
            savedTagRefs = tagRefs
            savedMemoBatches += SavedMemoBatch(memo, tagRefs)
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
            return movedToTrashCount
        }

        override suspend fun restoreMemoFromTrash(id: String): Int {
            restoredMemoId = id
            return restoredCount
        }

        override suspend fun deleteMemoPermanently(id: String): Int {
            permanentlyDeletedMemoId = id
            return deletedPermanentlyCount
        }

        override suspend fun discardMemo(id: String): Int {
            discardedMemoId = id
            return discardedCount
        }

        override suspend fun deleteTrashedMemosDeletedAtOrBefore(cutoff: Long) {
            purgeCutoff = cutoff
        }

        override suspend fun getAllActiveMemosWithTagRefs(): List<MemoWithTagRefs> =
            memosWithTagRefs.value.filter { it.memo.deletedAt == null }

        override suspend fun upsertAllMemosWithTags(
            memos: List<MemoEntity>,
            tagRefsByMemoId: Map<String, List<MemoTagRefEntity>>
        ) {
            onUpsertAllMemosWithTags?.invoke()
            memos.forEach { memo ->
                val refs = tagRefsByMemoId[memo.id] ?: emptyList()
                upsertMemoWithTags(memo, refs)
            }
        }
    }

    private class FakeTagDao(private val onUpsertAllTags: (() -> Unit)? = null) : TagDao {

        val upsertedTags = mutableListOf<TagEntity>()

        override fun observeTags(): Flow<List<TagEntity>> = MutableStateFlow(emptyList())
        override suspend fun getTag(id: String): TagEntity? = null
        override suspend fun getTagsByIds(ids: List<String>): List<TagEntity> = emptyList()
        override suspend fun upsertTag(tag: TagEntity) {}
        override suspend fun deleteTag(id: String) {}
        override suspend fun getAllTags(): List<TagEntity> = emptyList()

        override suspend fun upsertAllTags(tags: List<TagEntity>) {
            onUpsertAllTags?.invoke()
            upsertedTags += tags
        }
    }

}
