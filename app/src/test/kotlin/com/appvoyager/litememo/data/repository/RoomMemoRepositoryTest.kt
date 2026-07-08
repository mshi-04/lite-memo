package com.appvoyager.litememo.data.repository

import androidx.room.InvalidationTracker
import com.appvoyager.litememo.data.local.LiteMemoDatabase
import com.appvoyager.litememo.data.local.dao.MemoDao
import com.appvoyager.litememo.data.local.dao.TagDao
import com.appvoyager.litememo.data.local.entity.MemoEntity
import com.appvoyager.litememo.data.local.entity.MemoImageEntity
import com.appvoyager.litememo.data.local.entity.MemoTagRefEntity
import com.appvoyager.litememo.data.local.entity.TagEntity
import com.appvoyager.litememo.data.local.model.MemoWithRefs
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.memoImageFixture
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.MemoImageFileName
import com.appvoyager.litememo.domain.model.value.SearchQuery
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.repository.MemoImageStore
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
            memosWithRefs = listOf(
                memoWithRefs(
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
            memosWithRefs = listOf(
                memoWithRefs(
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
            memosWithRefs = listOf(memoWithRefs(memoId = "memo-1"))
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
            memosWithRefs = listOf(
                memoWithRefs(memoId = "memo-before", createdAt = 999L),
                memoWithRefs(memoId = "memo-start", createdAt = 1000L),
                memoWithRefs(memoId = "memo-end", createdAt = 2000L),
                memoWithRefs(memoId = "memo-after", createdAt = 2001L)
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
            memosWithRefs = listOf(memoWithRefs(memoId = "memo-1", createdAt = 1_000L))
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
    fun interactionSaveMemoWritesImageRefsToDao() = runTest {
        // Arrange
        val dao = FakeMemoDao()
        val repository = createRepository(dao)

        // Act
        // Interaction: image order is delegated to DAO refs.
        repository.saveMemo(
            memoFixture(
                id = "memo-1",
                images = listOf(
                    memoImageFixture(id = "image-1", fileName = "image-1.jpg"),
                    memoImageFixture(id = "image-2", fileName = "image-2.png")
                )
            )
        )

        // Assert
        assertEquals(
            listOf(
                MemoImageEntity(
                    id = "image-1",
                    memoId = "memo-1",
                    fileName = "image-1.jpg",
                    position = 0
                ),
                MemoImageEntity(
                    id = "image-2",
                    memoId = "memo-1",
                    fileName = "image-2.png",
                    position = 1
                )
            ),
            dao.savedImageRefs
        )
    }

    @Test
    fun interactionSaveMemoDeletesRemovedImageFilesAfterUpsert() = runTest {
        // Arrange
        val dao = FakeMemoDao(
            imageFileNamesByMemoId = mutableMapOf("memo-1" to listOf("old.jpg", "keep.jpg"))
        )
        val imageStore = FakeMemoImageStore()
        val repository = createRepository(dao, imageStore)

        // Act
        // Interaction: files removed from DB refs are deleted after memo upsert.
        repository.saveMemo(
            memoFixture(
                id = "memo-1",
                images = listOf(memoImageFixture(id = "image-keep", fileName = "keep.jpg"))
            )
        )

        // Assert
        assertEquals(listOf(MemoImageFileName("old.jpg")), imageStore.deletedFileNames)
    }

    @Test
    fun interactionSaveMemoDoesNotDeleteFilesWhenImagesUnchanged() = runTest {
        // Arrange
        val dao = FakeMemoDao(
            imageFileNamesByMemoId = mutableMapOf("memo-1" to listOf("keep.jpg"))
        )
        val imageStore = FakeMemoImageStore()
        val repository = createRepository(dao, imageStore)

        // Act
        // Interaction: unchanged image refs do not touch file storage.
        repository.saveMemo(
            memoFixture(
                id = "memo-1",
                images = listOf(memoImageFixture(id = "image-keep", fileName = "keep.jpg"))
            )
        )

        // Assert
        assertEquals(emptyList<MemoImageFileName>(), imageStore.deletedFileNames)
    }

    @Test
    fun observeTrashedMemosReturnsOnlyTrashedMemosOrderedByDeletedAtDescending() = runTest {
        // Arrange
        val dao = FakeMemoDao(
            memosWithRefs = listOf(
                memoWithRefs(memoId = "memo-active"),
                memoWithRefs(memoId = "memo-old", deletedAt = 1_000L),
                memoWithRefs(memoId = "memo-new", deletedAt = 2_000L)
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
    fun interactionDeleteMemoPermanentlyDeletesImageFiles() = runTest {
        // Arrange
        val dao = FakeMemoDao(
            imageFileNamesByMemoId = mutableMapOf("memo-1" to listOf("image-1.jpg"))
        )
        val imageStore = FakeMemoImageStore()
        val repository = createRepository(dao, imageStore)

        // Act
        // Interaction: permanent delete removes files after deleting DB rows.
        repository.deleteMemoPermanently(MemoId("memo-1"))

        // Assert
        assertEquals(listOf(MemoImageFileName("image-1.jpg")), imageStore.deletedFileNames)
    }

    @Test
    fun errorDeleteMemoPermanentlyDoesNotDeleteFilesWhenMemoMissing() = runTest {
        // Arrange
        val dao = FakeMemoDao(
            deletedPermanentlyCount = 0,
            imageFileNamesByMemoId = mutableMapOf("memo-1" to listOf("image-1.jpg"))
        )
        val imageStore = FakeMemoImageStore()
        val repository = createRepository(dao, imageStore)

        // Act
        // Error: failed DB delete must not remove files.
        val error = runCatching { repository.deleteMemoPermanently(MemoId("memo-1")) }
            .exceptionOrNull()

        // Assert
        assertEquals(IllegalStateException::class.java, error?.javaClass)
        assertEquals(emptyList<MemoImageFileName>(), imageStore.deletedFileNames)
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
    fun interactionDiscardMemoDeletesImageFiles() = runTest {
        // Arrange
        val dao = FakeMemoDao(
            imageFileNamesByMemoId = mutableMapOf("memo-1" to listOf("image-1.jpg"))
        )
        val imageStore = FakeMemoImageStore()
        val repository = createRepository(dao, imageStore)

        // Act
        // Interaction: discard removes files after deleting DB rows.
        repository.discardMemo(MemoId("memo-1"))

        // Assert
        assertEquals(listOf(MemoImageFileName("image-1.jpg")), imageStore.deletedFileNames)
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
    fun interactionPurgeDeletesImageFilesOfExpiredTrashedMemos() = runTest {
        // Arrange
        val dao = FakeMemoDao(
            imageFileNamesForPurge = listOf("old-1.jpg", "old-2.jpg")
        )
        val imageStore = FakeMemoImageStore()
        val repository = createRepository(dao, imageStore)

        // Act
        // Interaction: purge uses the DAO-provided file list after the transaction.
        repository.deleteTrashedMemosDeletedAtOrBefore(TimestampMillis(2_000L))

        // Assert
        assertEquals(
            listOf(MemoImageFileName("old-1.jpg"), MemoImageFileName("old-2.jpg")),
            imageStore.deletedFileNames
        )
    }

    @Test
    fun interactionSaveAllMemosDeletesRemovedImageFilesAfterUpsert() = runTest {
        // Arrange
        val dao = FakeMemoDao(
            imageFileNamesByMemoId = mutableMapOf("memo-1" to listOf("old.jpg", "keep.jpg"))
        )
        val imageStore = FakeMemoImageStore()
        val repository = createRepository(dao, imageStore)

        // Act
        // Interaction: bulk save deletes files dropped from DB refs after upsert.
        repository.saveAllMemos(
            listOf(
                memoFixture(
                    id = "memo-1",
                    images = listOf(memoImageFixture(id = "image-keep", fileName = "keep.jpg"))
                )
            )
        )

        // Assert
        assertEquals(listOf(MemoImageFileName("old.jpg")), imageStore.deletedFileNames)
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
    fun interactionExecuteImportReturnsImageFilesOfOverwrittenMemos() = runTest {
        // Arrange
        val memoDao = FakeMemoDao(
            imageFileNamesByMemoId = mutableMapOf("m1" to listOf("old.jpg"))
        )
        val tagDao = FakeTagDao()
        val repository = createRepositoryForImport(memoDao, tagDao)
        val memo = memoFixture(id = "m1")

        // Act
        // Interaction: import overwrites memos without images and reports removed files.
        val removedFileNames = repository.executeImport(tags = emptyList(), memos = listOf(memo))

        // Assert
        assertEquals(listOf("old.jpg"), removedFileNames)
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
        val memoDao = FakeMemoDao(onUpsertAllMemosWithRefs = { callOrder += "memos" })
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
        return RoomMemoRepository(memoDao, tagDao, dummyDatabase, FakeMemoImageStore())
    }

    private fun createRepository(
        dao: FakeMemoDao,
        imageStore: FakeMemoImageStore = FakeMemoImageStore()
    ): RoomMemoRepository {
        // database is only used by importAll(), which is not tested in this unit test.
        val dummyDatabase = object : LiteMemoDatabase() {
            override fun memoDao(): MemoDao = dao
            override fun tagDao(): TagDao = throw UnsupportedOperationException()
            override fun createInvalidationTracker(): InvalidationTracker =
                throw UnsupportedOperationException()
            override fun clearAllTables() = throw UnsupportedOperationException()
        }
        return RoomMemoRepository(dao, FakeTagDao(), dummyDatabase, imageStore)
    }

    private fun memoWithRefs(
        memoId: String,
        createdAt: Long = 1000L,
        updatedAt: Long = 1000L,
        tagRefs: List<MemoTagRefEntity> = emptyList(),
        deletedAt: Long? = null
    ) = MemoWithRefs(
        memo = MemoEntity(
            id = memoId,
            title = "Title",
            body = "Body",
            createdAt = createdAt,
            updatedAt = updatedAt,
            isFavorite = false,
            deletedAt = deletedAt
        ),
        tagRefs = tagRefs,
        imageRefs = emptyList()
    )

    private data class MovedToTrashRecord(val memoId: String, val deletedAt: Long)

    private data class ObservedRange(val fromMillis: Long, val toMillis: Long)

    private data class SavedMemoBatch(
        val memo: MemoEntity,
        val tagRefs: List<MemoTagRefEntity>,
        val imageRefs: List<MemoImageEntity>
    )

    private data class NoImportWritesSnapshot(
        val upsertedTagCount: Int = 0,
        val savedMemoBatchCount: Int = 0
    )

    private fun importWritesSnapshot(tagDao: FakeTagDao, memoDao: FakeMemoDao) =
        NoImportWritesSnapshot(
            upsertedTagCount = tagDao.upsertedTags.size,
            savedMemoBatchCount = memoDao.savedMemoBatches.size
        )

    @Suppress("LongParameterList")
    private class FakeMemoDao(
        memosWithRefs: List<MemoWithRefs> = emptyList(),
        private val imageFileNamesByMemoId: MutableMap<String, List<String>> = mutableMapOf(),
        private val imageFileNamesForPurge: List<String> = emptyList(),
        private val failOnObserveMemosBetween: Boolean = false,
        private val movedToTrashCount: Int = 1,
        private val restoredCount: Int = 1,
        private val deletedPermanentlyCount: Int = 1,
        private val onUpsertAllMemosWithRefs: (() -> Unit)? = null
    ) : MemoDao {

        private val memosWithRefs = MutableStateFlow(memosWithRefs)
        var savedMemo: MemoEntity? = null
        var savedTagRefs: List<MemoTagRefEntity> = emptyList()
        var savedImageRefs: List<MemoImageEntity> = emptyList()
        val savedMemoBatches = mutableListOf<SavedMemoBatch>()
        var movedToTrash: MovedToTrashRecord? = null
        var restoredMemoId: String? = null
        var permanentlyDeletedMemoId: String? = null
        var discardedMemoId: String? = null
        var discardedCount: Int = 1
        var purgeCutoff: Long? = null
        var observedRange: ObservedRange? = null
        var observedSearchPattern: String? = null

        override fun observeActiveMemosWithRefs(): Flow<List<MemoWithRefs>> =
            memosWithRefs.map { list -> list.filter { it.memo.deletedAt == null } }

        override fun observeActiveMemosWithRefsBySearchPattern(
            pattern: String
        ): Flow<List<MemoWithRefs>> {
            observedSearchPattern = pattern
            return memosWithRefs.map { list -> list.filter { it.memo.deletedAt == null } }
        }

        override fun observeActiveMemosWithRefsCreatedBetween(
            fromMillis: Long,
            toMillis: Long
        ): Flow<List<MemoWithRefs>> {
            if (failOnObserveMemosBetween) {
                fail<Nothing>("observeActiveMemosWithRefsCreatedBetween should not be called.")
            }
            observedRange = ObservedRange(fromMillis = fromMillis, toMillis = toMillis)
            return memosWithRefs.map { list ->
                list.filter {
                    it.memo.deletedAt == null &&
                        it.memo.createdAt >= fromMillis &&
                        it.memo.createdAt < toMillis
                }
            }
        }

        override suspend fun getActiveMemoWithRefs(id: String): MemoWithRefs? =
            memosWithRefs.value.firstOrNull { it.memo.id == id && it.memo.deletedAt == null }

        override fun observeTrashedMemosWithRefs(): Flow<List<MemoWithRefs>> =
            memosWithRefs.map { list ->
                list
                    .filter { it.memo.deletedAt != null }
                    .sortedByDescending { it.memo.deletedAt }
            }

        override suspend fun upsertMemoWithRefs(
            memo: MemoEntity,
            tagRefs: List<MemoTagRefEntity>,
            imageRefs: List<MemoImageEntity>
        ) {
            savedMemo = memo
            savedTagRefs = tagRefs
            savedImageRefs = imageRefs
            savedMemoBatches += SavedMemoBatch(memo, tagRefs, imageRefs)
            imageFileNamesByMemoId[memo.id] = imageRefs.map { it.fileName }
        }

        override suspend fun upsertMemo(memo: MemoEntity) {
            savedMemo = memo
        }

        override suspend fun insertTagRefs(tagRefs: List<MemoTagRefEntity>) {
            savedTagRefs = tagRefs
        }

        override suspend fun insertImageRefs(imageRefs: List<MemoImageEntity>) {
            savedImageRefs = imageRefs
        }

        override suspend fun deleteTagRefsForMemo(memoId: String) {
            savedTagRefs = emptyList()
        }

        override suspend fun deleteImageRefsForMemo(memoId: String) {
            savedImageRefs = emptyList()
            imageFileNamesByMemoId[memoId] = emptyList()
        }

        override suspend fun getImageFileNamesForMemo(memoId: String): List<String> =
            imageFileNamesByMemoId[memoId].orEmpty()

        override suspend fun getImageFileNamesForMemos(memoIds: List<String>): List<String> =
            memoIds.flatMap { imageFileNamesByMemoId[it].orEmpty() }

        override suspend fun getImageFileNamesForTrashedMemosDeletedAtOrBefore(
            cutoff: Long
        ): List<String> = imageFileNamesForPurge

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
            if (deletedPermanentlyCount > 0) {
                imageFileNamesByMemoId.remove(id)
            }
            return deletedPermanentlyCount
        }

        override suspend fun discardMemo(id: String): Int {
            discardedMemoId = id
            if (discardedCount > 0) {
                imageFileNamesByMemoId.remove(id)
            }
            return discardedCount
        }

        override suspend fun deleteTrashedMemosDeletedAtOrBefore(cutoff: Long) {
            purgeCutoff = cutoff
        }

        override suspend fun getAllActiveMemosWithRefs(): List<MemoWithRefs> =
            memosWithRefs.value.filter { it.memo.deletedAt == null }

        override suspend fun upsertAllMemosWithRefs(
            memos: List<MemoEntity>,
            tagRefsByMemoId: Map<String, List<MemoTagRefEntity>>,
            imageRefsByMemoId: Map<String, List<MemoImageEntity>>
        ) {
            onUpsertAllMemosWithRefs?.invoke()
            super.upsertAllMemosWithRefs(memos, tagRefsByMemoId, imageRefsByMemoId)
        }
    }

    private class FakeMemoImageStore : MemoImageStore {

        val deletedFileNames = mutableListOf<MemoImageFileName>()

        override suspend fun saveImage(
            source: com.appvoyager.litememo.domain.model.value.ImageSourceReference
        ) = error("saveImage is not used by RoomMemoRepositoryTest.")

        override suspend fun deleteImages(fileNames: List<MemoImageFileName>) {
            deletedFileNames += fileNames
        }

        override fun resolveImagePath(fileName: MemoImageFileName): String =
            error("resolveImagePath is not used by RoomMemoRepositoryTest.")
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
