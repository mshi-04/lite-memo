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
import com.appvoyager.litememo.domain.provider.CurrentTimeProvider
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.repository.TagRepository
import com.appvoyager.litememo.domain.tagFixture
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
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
    fun invokeKeepsExistingUpdatedAtForTrashWhenCurrentTimeIsEarlierThanUpdatedAt() = runTest {
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
                action = MemoBulkAction.moveToTrash()
            )
        )

        // Assert
        assertEquals(TimestampMillis(5000L), repository.movedToTrash.single().deletedAt)
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
    fun boundarySetFavoriteDeduplicatesMemoIdsBeforeSaving() = runTest {
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
                action = MemoBulkAction.setFavorite(true)
            )
        )

        // Assert
        assertEquals(
            listOf(MemoId("memo-2"), MemoId("memo-1")),
            repository.savedMemos.map {
                it.id
            }
        )
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
    fun boundaryNoOpBulkChangesPersistNoChangedMemos() = runTest {
        // Arrange
        val tagId = TagId("tag-1")
        val alreadyFavoriteRepository = FakeMemoRepository(
            listOf(memoFixture(id = "favorite", isFavorite = true))
        )
        val alreadyTaggedRepository = FakeMemoRepository(
            listOf(memoFixture(id = "tagged", tagIds = listOf(tagId)))
        )
        val untaggedRepository = FakeMemoRepository(
            listOf(memoFixture(id = "untagged"))
        )
        val tagRepository = FakeTagRepository(listOf(tagFixture(id = tagId.value)))

        // Act
        // Boundary/Normal: no-op favorite/tag changes should not produce changed memos.
        applyMemoBulkActionUseCase(
            memoRepository = alreadyFavoriteRepository
        )(
            ApplyMemoBulkActionCommand(
                memoIds = listOf(MemoId("favorite")),
                action = MemoBulkAction.setFavorite(true)
            )
        )
        applyMemoBulkActionUseCase(
            memoRepository = alreadyTaggedRepository,
            tagRepository = tagRepository
        )(
            ApplyMemoBulkActionCommand(
                memoIds = listOf(MemoId("tagged")),
                action = MemoBulkAction.addTag(tagId)
            )
        )
        applyMemoBulkActionUseCase(
            memoRepository = untaggedRepository,
            tagRepository = tagRepository
        )(
            ApplyMemoBulkActionCommand(
                memoIds = listOf(MemoId("untagged")),
                action = MemoBulkAction.removeTag(tagId)
            )
        )

        // Assert
        assertEquals(
            NoOpBulkChangeSnapshot(
                alreadyFavoriteSavedMemos = emptyList(),
                alreadyTaggedSavedMemos = emptyList(),
                untaggedSavedMemos = emptyList()
            ),
            NoOpBulkChangeSnapshot(
                alreadyFavoriteSavedMemos = alreadyFavoriteRepository.savedMemos,
                alreadyTaggedSavedMemos = alreadyTaggedRepository.savedMemos,
                untaggedSavedMemos = untaggedRepository.savedMemos
            )
        )
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
    fun boundaryEmptyMemoIdsDoesNotReadTimeOrDependencies() = runTest {
        // Arrange
        val memoRepository = mockk<MemoRepository>()
        val tagRepository = mockk<TagRepository>()
        val timeProvider = mockk<CurrentTimeProvider>()
        val useCase = ApplyMemoBulkActionUseCase(
            memoRepository = memoRepository,
            tagRepository = tagRepository,
            currentTimeProvider = timeProvider
        )

        // Act
        // Boundary/Interaction: empty input is a pure no-op.
        useCase(
            ApplyMemoBulkActionCommand(
                memoIds = emptyList(),
                action = MemoBulkAction.addTag(TagId("tag-1"))
            )
        )

        // Assert
        coVerify(exactly = 0) { memoRepository.getActiveMemo(any()) }
        coVerify(exactly = 0) { tagRepository.getTag(any()) }
        verify(exactly = 0) { timeProvider.now() }
        confirmVerified(memoRepository, tagRepository, timeProvider)
    }

    @Test
    fun interactionMissingMemoDoesNotWriteAnyBulkAction() = runTest {
        // Arrange
        val memoRepository = mockk<MemoRepository>()
        val tagRepository = mockk<TagRepository>(relaxed = true)
        val timeProvider = mockk<CurrentTimeProvider>()
        coEvery { memoRepository.getActiveMemo(MemoId("missing")) } returns null
        val useCase = ApplyMemoBulkActionUseCase(
            memoRepository = memoRepository,
            tagRepository = tagRepository,
            currentTimeProvider = timeProvider
        )

        // Act
        val error = runCatching {
            useCase(
                ApplyMemoBulkActionCommand(
                    memoIds = listOf(MemoId("missing")),
                    action = MemoBulkAction.setFavorite(true)
                )
            )
        }.exceptionOrNull()

        // Assert
        assertEquals(IllegalArgumentException::class.java, error?.javaClass)
        coVerify(exactly = 1) { memoRepository.getActiveMemo(MemoId("missing")) }
        coVerify(exactly = 0) { memoRepository.saveAllMemos(any()) }
        confirmVerified(memoRepository, tagRepository)
    }

    @Test
    fun interactionMissingTagDoesNotWriteAnyBulkAction() = runTest {
        // Arrange
        val tagId = TagId("missing")
        val memo = memoFixture(id = "memo-1")
        val memoRepository = mockk<MemoRepository>()
        val tagRepository = mockk<TagRepository>()
        val timeProvider = mockk<CurrentTimeProvider>()
        coEvery { memoRepository.getActiveMemo(memo.id) } returns memo
        coEvery { tagRepository.getTag(tagId) } returns null
        val useCase = ApplyMemoBulkActionUseCase(
            memoRepository = memoRepository,
            tagRepository = tagRepository,
            currentTimeProvider = timeProvider
        )

        // Act
        val error = runCatching {
            useCase(
                ApplyMemoBulkActionCommand(
                    memoIds = listOf(memo.id),
                    action = MemoBulkAction.addTag(tagId)
                )
            )
        }.exceptionOrNull()

        // Assert
        assertEquals(IllegalArgumentException::class.java, error?.javaClass)
        coVerify(exactly = 1) { memoRepository.getActiveMemo(memo.id) }
        coVerify(exactly = 1) { tagRepository.getTag(tagId) }
        coVerify(exactly = 0) { memoRepository.saveAllMemos(any()) }
        confirmVerified(memoRepository, tagRepository)
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

    private data class NoOpBulkChangeSnapshot(
        val alreadyFavoriteSavedMemos: List<Memo>,
        val alreadyTaggedSavedMemos: List<Memo>,
        val untaggedSavedMemos: List<Memo>
    )
}
