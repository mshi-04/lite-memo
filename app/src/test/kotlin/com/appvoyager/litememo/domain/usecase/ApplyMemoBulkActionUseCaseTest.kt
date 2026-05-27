package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.FakeTagRepository
import com.appvoyager.litememo.domain.MutableTimeProvider
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.ApplyMemoBulkActionCommand
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.MemoBulkAction
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.SearchQuery
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.tagFixture
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ApplyMemoBulkActionUseCaseTest {

    @Test
    fun invokeMovesMemosToTrashInInputOrder() = runTest {
        // Arrange
        val repository = FakeMemoRepository(
            listOf(
                memoFixture(id = "memo-1"),
                memoFixture(id = "memo-2")
            )
        )
        val useCase = applyMemoBulkActionUseCase(memoRepository = repository)

        // Act
        useCase(
            ApplyMemoBulkActionCommand(
                memoIds = listOf(MemoId("memo-2"), MemoId("memo-1"), MemoId("memo-2")),
                action = MemoBulkAction.moveToTrash()
            )
        )

        // Assert
        val expectedIds = listOf(MemoId("memo-2"), MemoId("memo-1"))
        assertEquals(expectedIds, repository.movedToTrash.map { it.memoId })
    }

    @Test
    fun invokeSavesFavoriteUpdatesInInputOrder() = runTest {
        // Arrange
        val repository = FakeMemoRepository(
            listOf(
                memoFixture(id = "memo-1"),
                memoFixture(id = "memo-2")
            )
        )
        val useCase = applyMemoBulkActionUseCase(memoRepository = repository)

        // Act
        useCase(
            ApplyMemoBulkActionCommand(
                memoIds = listOf(MemoId("memo-2"), MemoId("memo-1")),
                action = MemoBulkAction.setFavorite(true)
            )
        )

        // Assert
        val expectedIds = listOf(MemoId("memo-2"), MemoId("memo-1"))
        assertEquals(expectedIds, repository.savedMemos.map { it.id })
    }

    @Test
    fun invokeAddsTagInInputOrder() = runTest {
        // Arrange
        val tagId = TagId("tag-1")
        val repository = FakeMemoRepository(
            listOf(
                memoFixture(id = "memo-1"),
                memoFixture(id = "memo-2")
            )
        )
        val useCase = applyMemoBulkActionUseCase(
            memoRepository = repository,
            tagRepository = FakeTagRepository(listOf(tagFixture(id = tagId.value)))
        )

        // Act
        useCase(
            ApplyMemoBulkActionCommand(
                memoIds = listOf(MemoId("memo-2"), MemoId("memo-1")),
                action = MemoBulkAction.addTag(tagId)
            )
        )

        // Assert
        val expectedIds = listOf(MemoId("memo-2"), MemoId("memo-1"))
        assertEquals(expectedIds, repository.savedMemos.map { it.id })
    }

    @Test
    fun invokeRemovesTagInInputOrder() = runTest {
        // Arrange
        val tagId = TagId("tag-1")
        val repository = FakeMemoRepository(
            listOf(
                memoFixture(id = "memo-1", tagIds = listOf(tagId)),
                memoFixture(id = "memo-2", tagIds = listOf(tagId))
            )
        )
        val useCase = applyMemoBulkActionUseCase(
            memoRepository = repository,
            tagRepository = FakeTagRepository(listOf(tagFixture(id = tagId.value)))
        )

        // Act
        useCase(
            ApplyMemoBulkActionCommand(
                memoIds = listOf(MemoId("memo-2"), MemoId("memo-1")),
                action = MemoBulkAction.removeTag(tagId)
            )
        )

        // Assert
        val expectedIds = listOf(MemoId("memo-2"), MemoId("memo-1"))
        assertEquals(expectedIds, repository.savedMemos.map { it.id })
    }

    @Test
    fun invokeThrowsBeforeWritingWhenMemoIsMissing() = runTest {
        // Arrange
        val repository = FakeMemoRepository(listOf(memoFixture(id = "memo-1")))
        val useCase = applyMemoBulkActionUseCase(memoRepository = repository)

        // Act
        val error = runCatching {
            useCase(
                ApplyMemoBulkActionCommand(
                    memoIds = listOf(MemoId("memo-1"), MemoId("missing")),
                    action = MemoBulkAction.setFavorite(true)
                )
            )
        }.exceptionOrNull()

        // Assert
        val expected = true to emptyList<Memo>()
        val actual = (error is IllegalArgumentException) to repository.savedMemos
        assertEquals(expected, actual)
    }

    @Test
    fun invokeThrowsBeforeWritingWhenTagIsMissing() = runTest {
        // Arrange
        val repository = FakeMemoRepository(listOf(memoFixture(id = "memo-1")))
        val useCase = applyMemoBulkActionUseCase(
            memoRepository = repository,
            tagRepository = FakeTagRepository()
        )

        // Act
        val error = runCatching {
            useCase(
                ApplyMemoBulkActionCommand(
                    memoIds = listOf(MemoId("memo-1")),
                    action = MemoBulkAction.addTag(TagId("missing"))
                )
            )
        }.exceptionOrNull()

        // Assert
        val expected = true to emptyList<Memo>()
        val actual = (error is IllegalArgumentException) to repository.savedMemos
        assertEquals(expected, actual)
    }

    @Test
    fun invokeStopsSavingAfterFirstWriteFailure() = runTest {
        // Arrange
        val repository = WriteFailingMemoRepository(
            memos = listOf(
                memoFixture(id = "memo-1"),
                memoFixture(id = "memo-2"),
                memoFixture(id = "memo-3")
            ),
            saveFailureId = MemoId("memo-2")
        )
        val useCase = applyMemoBulkActionUseCase(memoRepository = repository)

        // Act
        runCatching {
            useCase(
                ApplyMemoBulkActionCommand(
                    memoIds = listOf(MemoId("memo-1"), MemoId("memo-2"), MemoId("memo-3")),
                    action = MemoBulkAction.setFavorite(true)
                )
            )
        }

        // Assert
        assertEquals(listOf(MemoId("memo-1")), repository.savedMemos.map { it.id })
    }

    @Test
    fun invokeStopsMovingAfterFirstWriteFailure() = runTest {
        // Arrange
        val repository = WriteFailingMemoRepository(
            memos = listOf(
                memoFixture(id = "memo-1"),
                memoFixture(id = "memo-2"),
                memoFixture(id = "memo-3")
            ),
            moveFailureId = MemoId("memo-2")
        )
        val useCase = applyMemoBulkActionUseCase(memoRepository = repository)

        // Act
        runCatching {
            useCase(
                ApplyMemoBulkActionCommand(
                    memoIds = listOf(MemoId("memo-1"), MemoId("memo-2"), MemoId("memo-3")),
                    action = MemoBulkAction.moveToTrash()
                )
            )
        }

        // Assert
        assertEquals(listOf(MemoId("memo-1")), repository.movedMemoIds)
    }

    private fun applyMemoBulkActionUseCase(
        memoRepository: MemoRepository = FakeMemoRepository(),
        tagRepository: FakeTagRepository = FakeTagRepository(),
        now: TimestampMillis = TimestampMillis(2_000L)
    ) = ApplyMemoBulkActionUseCase(
        memoRepository = memoRepository,
        tagRepository = tagRepository,
        currentTimeProvider = MutableTimeProvider(now)
    )

    private class WriteFailingMemoRepository(
        memos: List<Memo>,
        private val saveFailureId: MemoId? = null,
        private val moveFailureId: MemoId? = null
    ) : MemoRepository {

        private val memosById = memos.associateBy { it.id }.toMutableMap()
        val savedMemos = mutableListOf<Memo>()
        val movedMemoIds = mutableListOf<MemoId>()

        override fun observeActiveMemos(): Flow<List<Memo>> =
            flowOf(memosById.values.filter { it.deletedAt == null })

        override fun observeActiveMemosBySearchQuery(query: SearchQuery): Flow<List<Memo>> =
            flowOf(emptyList())

        override fun observeActiveMemosCreatedBetween(
            from: TimestampMillis,
            to: TimestampMillis
        ): Flow<List<Memo>> = flowOf(emptyList())

        override fun observeTrashedMemos(): Flow<List<Memo>> = flowOf(emptyList())

        override suspend fun getActiveMemo(id: MemoId): Memo? = memosById[id]

        override suspend fun saveMemo(memo: Memo) {
            if (memo.id == saveFailureId) {
                throw IllegalStateException("Failed to save memo.")
            }
            savedMemos += memo
            memosById[memo.id] = memo
        }

        override suspend fun moveMemoToTrash(id: MemoId, deletedAt: TimestampMillis) {
            if (id == moveFailureId) {
                throw IllegalStateException("Failed to move memo.")
            }
            movedMemoIds += id
        }

        override suspend fun restoreMemoFromTrash(id: MemoId) = Unit

        override suspend fun deleteMemoPermanently(id: MemoId) = Unit

        override suspend fun deleteTrashedMemosDeletedAtOrBefore(cutoff: TimestampMillis) = Unit

        override suspend fun getAllActiveMemos(): List<Memo> =
            memosById.values.filter { it.deletedAt == null }

        override suspend fun saveAllMemos(memos: List<Memo>) = Unit
    }
}
