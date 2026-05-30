package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.FakeTagRepository
import com.appvoyager.litememo.domain.MutableTimeProvider
import com.appvoyager.litememo.domain.QueueMemoIdProvider
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.SaveMemoCommand
import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.MemoTitle
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.tagFixture
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SaveMemoUseCaseTest {

    @Test
    fun invokeCreatesMemoWithGeneratedId() = runTest {
        // Arrange
        val useCase =
            saveMemoUseCase(memoIdProvider = QueueMemoIdProvider(listOf(MemoId("generated-id"))))

        // Act
        val memo = useCase(SaveMemoCommand(title = MemoTitle("Title"), body = MemoBody("Body")))

        // Assert
        assertEquals(MemoId("generated-id"), memo.id)
    }

    @Test
    fun invokeCreatesMemoWithMatchingCreatedAtAndUpdatedAt() = runTest {
        // Arrange
        val useCase = saveMemoUseCase(timeProvider = MutableTimeProvider(TimestampMillis(2000L)))

        // Act
        val memo = useCase(SaveMemoCommand(title = MemoTitle("Title"), body = MemoBody("Body")))

        // Assert
        assertEquals(memo.createdAt, memo.updatedAt)
    }

    @Test
    fun invokePreservesCreatedAtWhenUpdatingExistingMemo() = runTest {
        // Arrange
        val existing = memoFixture(id = "memo-1", createdAt = 1000L, updatedAt = 1500L)
        val useCase = saveMemoUseCase(
            memoRepository = FakeMemoRepository(listOf(existing)),
            timeProvider = MutableTimeProvider(TimestampMillis(3000L))
        )

        // Act
        val memo =
            useCase(
                SaveMemoCommand(
                    id = existing.id,
                    title = MemoTitle("New"),
                    body = MemoBody("Body")
                )
            )

        // Assert
        assertEquals(TimestampMillis(1000L), memo.createdAt)
    }

    @Test
    fun invokeUpdatesUpdatedAtWhenUpdatingExistingMemo() = runTest {
        // Arrange
        val existing = memoFixture(id = "memo-1", createdAt = 1000L, updatedAt = 1500L)
        val useCase = saveMemoUseCase(
            memoRepository = FakeMemoRepository(listOf(existing)),
            timeProvider = MutableTimeProvider(TimestampMillis(3000L))
        )

        // Act
        val memo =
            useCase(
                SaveMemoCommand(
                    id = existing.id,
                    title = MemoTitle("New"),
                    body = MemoBody("Body")
                )
            )

        // Assert
        assertEquals(TimestampMillis(3000L), memo.updatedAt)
    }

    @Test
    fun invokeThrowsWhenMemoIdDoesNotExist() {
        // Arrange
        val useCase = saveMemoUseCase()

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            runTest {
                useCase(
                    SaveMemoCommand(
                        id = MemoId("missing-id"),
                        title = MemoTitle("Title"),
                        body = MemoBody("Body")
                    )
                )
            }
        }
    }

    @Test
    fun invokeThrowsWhenTitleAndBodyAreBlank() {
        // Arrange
        val useCase = saveMemoUseCase()

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            runTest {
                useCase(SaveMemoCommand(title = MemoTitle(" "), body = MemoBody(" ")))
            }
        }
    }

    @Test
    fun invokeDoesNotSaveMemoWhenTitleAndBodyAreBlank() = runTest {
        // Arrange
        val repository = FakeMemoRepository()
        val useCase = saveMemoUseCase(memoRepository = repository)

        // Act
        try {
            useCase(SaveMemoCommand(title = MemoTitle(" "), body = MemoBody(" ")))
        } catch (_: IllegalArgumentException) {
        }

        // Assert
        assertEquals(emptyList<Any>(), repository.savedMemos)
    }

    @Test
    fun invokeReturnsMemoWithUniqueTagIdsWhenDuplicateTagIdsAreProvided() = runTest {
        // Arrange
        val tagId = TagId("tag-1")
        val useCase =
            saveMemoUseCase(tagRepository = FakeTagRepository(listOf(tagFixture(id = tagId.value))))

        // Act
        val memo =
            useCase(
                SaveMemoCommand(
                    title = MemoTitle("Title"),
                    body = MemoBody("Body"),
                    tagIds = listOf(tagId, tagId)
                )
            )

        // Assert
        assertEquals(listOf(tagId), memo.tagIds)
    }

    @Test
    fun invokeThrowsWhenTagIdDoesNotExist() {
        // Arrange
        val repository = FakeMemoRepository()
        val useCase = saveMemoUseCase(memoRepository = repository)

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            runTest {
                useCase(
                    SaveMemoCommand(
                        title = MemoTitle("Title"),
                        body = MemoBody("Body"),
                        tagIds = listOf(TagId("missing"))
                    )
                )
            }
        }
    }

    @Test
    fun invokeUsesCommandCreatedAtWhenProvided() = runTest {
        // Arrange
        val useCase = saveMemoUseCase(timeProvider = MutableTimeProvider(TimestampMillis(5000L)))

        // Act
        val memo = useCase(
            SaveMemoCommand(
                title = MemoTitle("Title"),
                body = MemoBody("Body"),
                createdAt = TimestampMillis(1000L)
            )
        )

        // Assert
        assertEquals(TimestampMillis(1000L), memo.createdAt)
    }

    @Test
    fun invokeUsesCurrentTimeWhenCommandCreatedAtIsNull() = runTest {
        // Arrange
        val useCase = saveMemoUseCase(timeProvider = MutableTimeProvider(TimestampMillis(5000L)))

        // Act
        val memo = useCase(
            SaveMemoCommand(
                title = MemoTitle("Title"),
                body = MemoBody("Body")
            )
        )

        // Assert
        assertEquals(TimestampMillis(5000L), memo.createdAt)
    }

    @Test
    fun invokePreservesExistingCreatedAtEvenWhenCommandCreatedAtIsProvided() = runTest {
        // Arrange
        val existing = memoFixture(id = "memo-1", createdAt = 1000L, updatedAt = 1500L)
        val useCase = saveMemoUseCase(
            memoRepository = FakeMemoRepository(listOf(existing)),
            timeProvider = MutableTimeProvider(TimestampMillis(5000L))
        )

        // Act
        val memo = useCase(
            SaveMemoCommand(
                id = existing.id,
                title = MemoTitle("New"),
                body = MemoBody("Body"),
                createdAt = TimestampMillis(9000L)
            )
        )

        // Assert
        assertEquals(TimestampMillis(1000L), memo.createdAt)
    }

    @Test
    fun invokeDoesNotSaveMemoWhenTagIdDoesNotExist() = runTest {
        // Arrange
        val repository = FakeMemoRepository()
        val useCase = saveMemoUseCase(memoRepository = repository)

        // Act
        try {
            useCase(
                SaveMemoCommand(
                    title = MemoTitle("Title"),
                    body = MemoBody("Body"),
                    tagIds = listOf(TagId("missing"))
                )
            )
        } catch (_: IllegalArgumentException) {
        }

        // Assert
        assertEquals(emptyList<Any>(), repository.savedMemos)
    }

    private fun saveMemoUseCase(
        memoRepository: FakeMemoRepository = FakeMemoRepository(),
        tagRepository: FakeTagRepository = FakeTagRepository(),
        memoIdProvider: QueueMemoIdProvider = QueueMemoIdProvider(),
        timeProvider: MutableTimeProvider = MutableTimeProvider()
    ) = SaveMemoUseCase(
        memoRepository = memoRepository,
        tagRepository = tagRepository,
        memoIdProvider = memoIdProvider,
        currentTimeProvider = timeProvider
    )

}
