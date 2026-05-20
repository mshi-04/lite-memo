package com.appvoyager.litememo.data.local.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.appvoyager.litememo.data.local.LiteMemoDatabase
import com.appvoyager.litememo.data.local.entity.MemoEntity
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
    fun observeMemosWithTagRefsCreatedBetweenUsesInclusiveStartAndExclusiveEnd() = runTest {
        // Arrange
        memoDao.upsertMemo(memoEntity(id = "memo-before", createdAt = 999L))
        memoDao.upsertMemo(memoEntity(id = "memo-start", createdAt = 1_000L))
        memoDao.upsertMemo(memoEntity(id = "memo-end", createdAt = 2_000L))

        // Act
        val memos = memoDao.observeMemosWithTagRefsCreatedBetween(
            fromMillis = 1_000L,
            toMillis = 2_000L
        ).first()

        // Assert
        assertEquals(listOf("memo-start"), memos.map { it.memo.id })
    }

    @Test
    fun observeMemosWithTagRefsBySearchPatternMatchesTitleOrBody() = runTest {
        // Arrange
        memoDao.upsertMemo(memoEntity(id = "memo-title", title = "Trip plan"))
        memoDao.upsertMemo(memoEntity(id = "memo-body", body = "Trip notes"))
        memoDao.upsertMemo(memoEntity(id = "memo-other", title = "Shopping"))

        // Act
        val memos = memoDao.observeMemosWithTagRefsBySearchPattern("%Trip%").first()

        // Assert
        assertEquals(listOf("memo-title", "memo-body"), memos.map { it.memo.id })
    }

    @Test
    fun observeMemosWithTagRefsBySearchPatternMatchesAsciiIgnoringCase() = runTest {
        // Arrange
        memoDao.upsertMemo(memoEntity(id = "memo-1", title = "Shopping list"))

        // Act
        val memos = memoDao.observeMemosWithTagRefsBySearchPattern("%shopping%").first()

        // Assert
        assertEquals(listOf("memo-1"), memos.map { it.memo.id })
    }

    @Test
    fun observeMemosWithTagRefsBySearchPatternTreatsEscapedWildcardsAsLiteral() = runTest {
        // Arrange
        memoDao.upsertMemo(memoEntity(id = "memo-literal", title = "100%_done"))
        memoDao.upsertMemo(memoEntity(id = "memo-wildcard", title = "100xxdone"))

        // Act
        val memos = memoDao.observeMemosWithTagRefsBySearchPattern("%100\\%\\_done%").first()

        // Assert
        assertEquals(listOf("memo-literal"), memos.map { it.memo.id })
    }

    @Test
    fun observeMemosWithTagRefsBySearchPatternReturnsRelatedTagRefs() = runTest {
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
        val memo = memoDao.observeMemosWithTagRefsBySearchPattern("%Tagged%").first().single()

        // Assert
        assertEquals(listOf("tag-1"), memo.tagRefs.map { it.tagId })
    }

    @Test
    fun observeMemosWithTagRefsReturnsRelatedTagRefs() = runTest {
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
        val memo = memoDao.observeMemosWithTagRefs().first().single()

        // Assert
        assertEquals(listOf("tag-1"), memo.tagRefs.map { it.tagId })
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
            memoDao.observeMemosWithTagRefs().first().single().tagRefs
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

    private fun memoEntity(
        id: String,
        title: String = "Title",
        body: String = "Body",
        createdAt: Long = 1_000L
    ) = MemoEntity(
        id = id,
        title = title,
        body = body,
        createdAt = createdAt,
        updatedAt = createdAt,
        isImportant = false
    )

    private fun tagEntity(id: String, createdAt: Long = 1_000L) = TagEntity(
        id = id,
        name = "Tag",
        colorArgb = 0xFF6750A4,
        createdAt = createdAt
    )
}
