package com.appvoyager.litememo.data.local.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.appvoyager.litememo.data.local.LiteMemoDatabase
import com.appvoyager.litememo.data.local.entity.MemoEntity
import com.appvoyager.litememo.data.local.entity.MemoImageEntity
import com.appvoyager.litememo.data.local.entity.MemoTagRefEntity
import com.appvoyager.litememo.data.local.entity.TagEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDaoInstrumentedTest {

    private lateinit var database: LiteMemoDatabase
    private lateinit var memoDao: MemoDao
    private lateinit var tagDao: TagDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, LiteMemoDatabase::class.java).build()
        memoDao = database.memoDao()
        tagDao = database.tagDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeTagsReturnsTagsOrderedByCreatedAtThenId() = runTest {
        // Arrange
        tagDao.upsertTag(tagEntity(id = "tag-c", createdAt = 2_000L))
        tagDao.upsertTag(tagEntity(id = "tag-b", createdAt = 1_000L))
        tagDao.upsertTag(tagEntity(id = "tag-a", createdAt = 1_000L))

        // Act
        val tags = tagDao.observeTags().first()

        // Assert
        assertEquals(listOf("tag-a", "tag-b", "tag-c"), tags.map { it.id })
    }

    @Test
    fun observeActiveMemosWithRefsCreatedBetweenUsesInclusiveStartAndExclusiveEnd() = runTest {
        // Arrange
        memoDao.upsertMemo(memoEntity(id = "memo-before", createdAt = 999L))
        memoDao.upsertMemo(memoEntity(id = "memo-start", createdAt = 1_000L))
        memoDao.upsertMemo(memoEntity(id = "memo-end", createdAt = 2_000L))

        // Act
        val memos = memoDao.observeActiveMemosWithRefsCreatedBetween(
            fromMillis = 1_000L,
            toMillis = 2_000L
        ).first()

        // Assert
        assertEquals(listOf("memo-start"), memos.map { it.memo.id })
    }

    @Test
    fun observeActiveMemosWithRefsBySearchPatternMatchesTitleOrBody() = runTest {
        // Arrange
        memoDao.upsertMemo(memoEntity(id = "memo-title", title = "Trip plan"))
        memoDao.upsertMemo(memoEntity(id = "memo-body", body = "Trip notes"))
        memoDao.upsertMemo(memoEntity(id = "memo-other", title = "Shopping"))

        // Act
        val memos = memoDao.observeActiveMemosWithRefsBySearchPattern("%Trip%").first()

        // Assert
        assertEquals(listOf("memo-title", "memo-body"), memos.map { it.memo.id })
    }

    @Test
    fun observeActiveMemosWithRefsBySearchPatternMatchesAsciiIgnoringCase() = runTest {
        // Arrange
        memoDao.upsertMemo(memoEntity(id = "memo-1", title = "Shopping list"))

        // Act
        val memos = memoDao.observeActiveMemosWithRefsBySearchPattern("%shopping%").first()

        // Assert
        assertEquals(listOf("memo-1"), memos.map { it.memo.id })
    }

    @Test
    fun observeActiveMemosWithRefsBySearchPatternTreatsEscapedWildcardsAsLiteral() = runTest {
        // Arrange
        memoDao.upsertMemo(memoEntity(id = "memo-literal", title = "100%_done"))
        memoDao.upsertMemo(memoEntity(id = "memo-wildcard", title = "100xxdone"))

        // Act
        val memos = memoDao.observeActiveMemosWithRefsBySearchPattern("%100\\%\\_done%").first()

        // Assert
        assertEquals(listOf("memo-literal"), memos.map { it.memo.id })
    }

    @Test
    fun observeActiveMemosWithRefsBySearchPatternReturnsRelatedTagRefs() = runTest {
        // Arrange
        memoDao.upsertMemo(memoEntity(id = "memo-1", title = "Tagged memo"))
        tagDao.upsertTag(tagEntity(id = "tag-1"))
        memoDao.insertTagRefs(
            listOf(
                MemoTagRefEntity(
                    memoId = "memo-1",
                    tagId = "tag-1",
                    position = 0
                )
            )
        )

        // Act
        val memo = memoDao.observeActiveMemosWithRefsBySearchPattern("%Tagged%").first().single()

        // Assert
        assertEquals(listOf("tag-1"), memo.tagRefs.map { it.tagId })
    }

    @Test
    fun observeActiveMemosWithRefsReturnsRelatedTagRefs() = runTest {
        // Arrange
        memoDao.upsertMemo(memoEntity(id = "memo-1"))
        tagDao.upsertTag(tagEntity(id = "tag-1"))
        memoDao.insertTagRefs(
            listOf(
                MemoTagRefEntity(
                    memoId = "memo-1",
                    tagId = "tag-1",
                    position = 0
                )
            )
        )

        // Act
        val memo = memoDao.observeActiveMemosWithRefs().first().single()

        // Assert
        assertEquals(listOf("tag-1"), memo.tagRefs.map { it.tagId })
    }

    @Test
    fun observeActiveMemosWithRefsReturnsRelatedImageRefs() = runTest {
        // Arrange
        memoDao.upsertMemo(memoEntity(id = "memo-1"))
        memoDao.insertImageRefs(
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
            )
        )

        // Act
        val memo = memoDao.observeActiveMemosWithRefs().first().single()

        // Assert
        assertEquals(
            listOf("image-1.jpg", "image-2.png"),
            memo.imageRefs.sortedBy { it.position }.map { it.fileName }
        )
    }

    @Test
    fun upsertMemoWithRefsReplacesImageRefs() = runTest {
        // Arrange
        memoDao.upsertMemoWithRefs(
            memo = memoEntity(id = "memo-1"),
            tagRefs = emptyList(),
            imageRefs = listOf(
                MemoImageEntity(
                    id = "image-old",
                    memoId = "memo-1",
                    fileName = "old.jpg",
                    position = 0
                )
            )
        )

        // Act
        memoDao.upsertMemoWithRefs(
            memo = memoEntity(id = "memo-1"),
            tagRefs = emptyList(),
            imageRefs = listOf(
                MemoImageEntity(
                    id = "image-new",
                    memoId = "memo-1",
                    fileName = "new.jpg",
                    position = 0
                )
            )
        )
        val memo = memoDao.observeActiveMemosWithRefs().first().single()

        // Assert
        assertEquals(listOf("new.jpg"), memo.imageRefs.map { it.fileName })
    }

    @Test
    fun insertImageRefsThrowsWhenPositionsDuplicateForSameMemo() {
        assertThrows(SQLiteConstraintException::class.java) {
            runTest {
                // Arrange
                memoDao.upsertMemo(memoEntity(id = "memo-1"))
                val imageRefs = listOf(
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
                        position = 0
                    )
                )

                // Act
                memoDao.insertImageRefs(imageRefs)
            }
        }
    }

    @Test
    fun deleteTagCascadesDeleteToMemoTagRefs() = runTest {
        // Arrange
        memoDao.upsertMemo(memoEntity(id = "memo-1"))
        tagDao.upsertTag(tagEntity(id = "tag-1"))
        memoDao.insertTagRefs(
            listOf(
                MemoTagRefEntity(
                    memoId = "memo-1",
                    tagId = "tag-1",
                    position = 0
                )
            )
        )

        // Act
        tagDao.deleteTag("tag-1")

        // Assert
        assertEquals(
            emptyList<MemoTagRefEntity>(),
            memoDao.observeActiveMemosWithRefs().first().single().tagRefs
        )
    }

    @Test
    fun insertTagRefsThrowsWhenPositionsDuplicateForSameMemo() {
        assertThrows(SQLiteConstraintException::class.java) {
            runTest {
                // Arrange
                memoDao.upsertMemo(memoEntity(id = "memo-1"))
                tagDao.upsertTag(tagEntity(id = "tag-1"))
                tagDao.upsertTag(tagEntity(id = "tag-2"))
                val tagRefs = listOf(
                    MemoTagRefEntity(
                        memoId = "memo-1",
                        tagId = "tag-1",
                        position = 0
                    ),
                    MemoTagRefEntity(
                        memoId = "memo-1",
                        tagId = "tag-2",
                        position = 0
                    )
                )

                // Act
                memoDao.insertTagRefs(tagRefs)
            }
        }
    }

    @Test
    fun observeActiveMemosWithRefsExcludesTrashedMemos() = runTest {
        // Arrange
        memoDao.upsertMemo(memoEntity(id = "memo-active"))
        memoDao.upsertMemo(memoEntity(id = "memo-trashed", deletedAt = 2_000L))

        // Act
        val memos = memoDao.observeActiveMemosWithRefs().first()

        // Assert
        assertEquals(listOf("memo-active"), memos.map { it.memo.id })
    }

    @Test
    fun observeTrashedMemosWithRefsReturnsNewestDeletedFirst() = runTest {
        // Arrange
        memoDao.upsertMemo(memoEntity(id = "memo-old", deletedAt = 2_000L))
        memoDao.upsertMemo(memoEntity(id = "memo-active"))
        memoDao.upsertMemo(memoEntity(id = "memo-new", deletedAt = 3_000L))

        // Act
        val memos = memoDao.observeTrashedMemosWithRefs().first()

        // Assert
        assertEquals(listOf("memo-new", "memo-old"), memos.map { it.memo.id })
    }

    @Test
    fun restoreMemoFromTrashMovesMemoBackToActiveMemos() = runTest {
        // Arrange
        memoDao.upsertMemo(memoEntity(id = "memo-1", deletedAt = 2_000L))

        // Act
        memoDao.restoreMemoFromTrash("memo-1")
        val memos = memoDao.observeActiveMemosWithRefs().first()

        // Assert
        assertEquals(listOf("memo-1"), memos.map { it.memo.id })
    }

    @Test
    fun deleteMemoPermanentlyDoesNotDeleteActiveMemo() = runTest {
        // Arrange
        memoDao.upsertMemo(memoEntity(id = "memo-1"))

        // Act
        val deletedCount = memoDao.deleteMemoPermanently("memo-1")

        // Assert
        assertEquals(0, deletedCount)
    }

    @Test
    fun deleteMemoPermanentlyDeletesTrashedMemo() = runTest {
        // Arrange
        memoDao.upsertMemo(memoEntity(id = "memo-1", deletedAt = 2_000L))

        // Act
        memoDao.deleteMemoPermanently("memo-1")
        val memos = memoDao.observeTrashedMemosWithRefs().first()

        // Assert
        assertEquals(0, memos.size)
    }

    @Test
    fun discardMemoDeletesActiveMemo() = runTest {
        // Arrange
        memoDao.upsertMemo(memoEntity(id = "memo-1"))

        // Act
        val deletedCount = memoDao.discardMemo("memo-1")

        // Assert
        assertEquals(1, deletedCount)
    }

    @Test
    fun discardMemoCascadesDeleteToMemoTagRefs() = runTest {
        // Arrange
        memoDao.upsertMemo(memoEntity(id = "memo-1"))
        tagDao.upsertTag(tagEntity(id = "tag-1"))
        memoDao.insertTagRefs(
            listOf(MemoTagRefEntity(memoId = "memo-1", tagId = "tag-1", position = 0))
        )

        // Act
        memoDao.discardMemo("memo-1")
        memoDao.upsertMemo(memoEntity(id = "memo-1"))
        val memo = memoDao.observeActiveMemosWithRefs().first().single()

        // Assert
        assertEquals(emptyList<String>(), memo.tagRefs.map { it.tagId })
    }

    @Test
    fun discardMemoReturnsZeroWhenMemoDoesNotExist() = runTest {
        // Arrange
        val missingId = "missing"

        // Act
        val deletedCount = memoDao.discardMemo(missingId)

        // Assert
        assertEquals(0, deletedCount)
    }

    @Test
    fun deleteTrashedMemosDeletedAtOrBeforeUsesInclusiveCutoff() = runTest {
        // Arrange
        memoDao.upsertMemo(memoEntity(id = "memo-old", deletedAt = 1_000L))
        memoDao.upsertMemo(memoEntity(id = "memo-new", deletedAt = 1_001L))

        // Act
        memoDao.deleteTrashedMemosDeletedAtOrBefore(1_000L)
        val memos = memoDao.observeTrashedMemosWithRefs().first()

        // Assert
        assertEquals(listOf("memo-new"), memos.map { it.memo.id })
    }

    private fun memoEntity(
        id: String,
        title: String = "Title",
        body: String = "Body",
        createdAt: Long = 1_000L,
        deletedAt: Long? = null
    ) = MemoEntity(
        id = id,
        title = title,
        body = body,
        createdAt = createdAt,
        updatedAt = createdAt,
        isFavorite = false,
        deletedAt = deletedAt
    )

    private fun tagEntity(id: String, createdAt: Long = 1_000L) = TagEntity(
        id = id,
        name = "Tag",
        colorArgb = 0xFF6750A4,
        createdAt = createdAt
    )
}
