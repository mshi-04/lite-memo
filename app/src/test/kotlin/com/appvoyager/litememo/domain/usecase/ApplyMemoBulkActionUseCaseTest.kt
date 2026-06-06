package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.FakeTagRepository
import com.appvoyager.litememo.domain.MutableTimeProvider
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.ApplyMemoBulkActionCommand
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.MemoBulkAction
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.tagFixture
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
    fun invokeUsesCreatedAtForFavoriteUpdatedAtWhenCurrentTimeIsEarlierThanCreatedAt() = runTest {
        // Arrange
        val repository = FakeMemoRepository(listOf(memoFixture(id = "memo-1", createdAt = 3000L)))
        val useCase = applyMemoBulkActionUseCase(
            memoRepository = repository,
            now = TimestampMillis(2000L)
        )

        // Act
        useCase(
            ApplyMemoBulkActionCommand(
                memoIds = listOf(MemoId("memo-1")),
                action = MemoBulkAction.setFavorite(true)
            )
        )

        // Assert
        assertEquals(TimestampMillis(3000L), repository.savedMemos.single().updatedAt)
    }

    @Test
    fun invokeKeepsExistingUpdatedAtForFavoriteWhenCurrentTimeIsEarlierThanUpdatedAt() = runTest {
        // Arrange
        val repository = FakeMemoRepository(
            listOf(memoFixture(id = "memo-1", createdAt = 1000L, updatedAt = 5000L))
        )
        val useCase = applyMemoBulkActionUseCase(
            memoRepository = repository,
            now = TimestampMillis(3000L)
        )

        // Act
        useCase(
            ApplyMemoBulkActionCommand(
                memoIds = listOf(MemoId("memo-1")),
                action = MemoBulkAction.setFavorite(true)
            )
        )

        // Assert
        assertEquals(TimestampMillis(5000L), repository.savedMemos.single().updatedAt)
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
    fun invokeUsesCreatedAtForAddTagUpdatedAtWhenCurrentTimeIsEarlierThanCreatedAt() = runTest {
        // Arrange
        val tagId = TagId("tag-1")
        val repository = FakeMemoRepository(listOf(memoFixture(id = "memo-1", createdAt = 3000L)))
        val useCase = applyMemoBulkActionUseCase(
            memoRepository = repository,
            tagRepository = FakeTagRepository(listOf(tagFixture(id = tagId.value))),
            now = TimestampMillis(2000L)
        )

        // Act
        useCase(
            ApplyMemoBulkActionCommand(
                memoIds = listOf(MemoId("memo-1")),
                action = MemoBulkAction.addTag(tagId)
            )
        )

        // Assert
        assertEquals(TimestampMillis(3000L), repository.savedMemos.single().updatedAt)
    }

    @Test
    fun invokeKeepsExistingUpdatedAtForAddTagWhenCurrentTimeIsEarlierThanUpdatedAt() = runTest {
        // Arrange
        val tagId = TagId("tag-1")
        val repository = FakeMemoRepository(
            listOf(memoFixture(id = "memo-1", createdAt = 1000L, updatedAt = 5000L))
        )
        val useCase = applyMemoBulkActionUseCase(
            memoRepository = repository,
            tagRepository = FakeTagRepository(listOf(tagFixture(id = tagId.value))),
            now = TimestampMillis(3000L)
        )

        // Act
        useCase(
            ApplyMemoBulkActionCommand(
                memoIds = listOf(MemoId("memo-1")),
                action = MemoBulkAction.addTag(tagId)
            )
        )

        // Assert
        assertEquals(TimestampMillis(5000L), repository.savedMemos.single().updatedAt)
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
    fun invokeUsesCreatedAtForRemoveTagUpdatedAtWhenCurrentTimeIsEarlierThanCreatedAt() = runTest {
        // Arrange
        val tagId = TagId("tag-1")
        val repository = FakeMemoRepository(
            listOf(memoFixture(id = "memo-1", createdAt = 3000L, tagIds = listOf(tagId)))
        )
        val useCase = applyMemoBulkActionUseCase(
            memoRepository = repository,
            tagRepository = FakeTagRepository(listOf(tagFixture(id = tagId.value))),
            now = TimestampMillis(2000L)
        )

        // Act
        useCase(
            ApplyMemoBulkActionCommand(
                memoIds = listOf(MemoId("memo-1")),
                action = MemoBulkAction.removeTag(tagId)
            )
        )

        // Assert
        assertEquals(TimestampMillis(3000L), repository.savedMemos.single().updatedAt)
    }

    @Test
    fun invokeKeepsExistingUpdatedAtForRemoveTagWhenCurrentTimeIsEarlierThanUpdatedAt() = runTest {
        // Arrange
        val tagId = TagId("tag-1")
        val repository = FakeMemoRepository(
            listOf(
                memoFixture(
                    id = "memo-1",
                    createdAt = 1000L,
                    updatedAt = 5000L,
                    tagIds = listOf(tagId)
                )
            )
        )
        val useCase = applyMemoBulkActionUseCase(
            memoRepository = repository,
            tagRepository = FakeTagRepository(listOf(tagFixture(id = tagId.value))),
            now = TimestampMillis(3000L)
        )

        // Act
        useCase(
            ApplyMemoBulkActionCommand(
                memoIds = listOf(MemoId("memo-1")),
                action = MemoBulkAction.removeTag(tagId)
            )
        )

        // Assert
        assertEquals(TimestampMillis(5000L), repository.savedMemos.single().updatedAt)
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
        val delegate = FakeMemoRepository(
            listOf(
                memoFixture(id = "memo-1"),
                memoFixture(id = "memo-2"),
                memoFixture(id = "memo-3")
            )
        )
        val repository = WriteFailingMemoRepository(delegate, saveFailureId = MemoId("memo-2"))
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
        assertEquals(listOf(MemoId("memo-1")), delegate.savedMemos.map { it.id })
    }

    @Test
    fun invokeStopsMovingAfterFirstWriteFailure() = runTest {
        // Arrange
        val delegate = FakeMemoRepository(
            listOf(
                memoFixture(id = "memo-1"),
                memoFixture(id = "memo-2"),
                memoFixture(id = "memo-3")
            )
        )
        val repository = WriteFailingMemoRepository(delegate, moveFailureId = MemoId("memo-2"))
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
        assertEquals(listOf(MemoId("memo-1")), delegate.movedToTrash.map { it.memoId })
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
        private val delegate: FakeMemoRepository,
        private val saveFailureId: MemoId? = null,
        private val moveFailureId: MemoId? = null
    ) : MemoRepository by delegate {

        override suspend fun saveMemo(memo: Memo) {
            if (memo.id == saveFailureId) throw IllegalStateException("Failed to save memo.")
            delegate.saveMemo(memo)
        }

        override suspend fun saveAllMemos(memos: List<Memo>) {
            memos.forEach { saveMemo(it) }
        }

        override suspend fun moveMemoToTrash(id: MemoId, deletedAt: TimestampMillis) {
            if (id == moveFailureId) throw IllegalStateException("Failed to move memo.")
            delegate.moveMemoToTrash(id, deletedAt)
        }
    }
}
