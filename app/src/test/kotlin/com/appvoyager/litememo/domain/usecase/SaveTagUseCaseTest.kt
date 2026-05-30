package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.FakeTagRepository
import com.appvoyager.litememo.domain.MutableTimeProvider
import com.appvoyager.litememo.domain.QueueTagIdProvider
import com.appvoyager.litememo.domain.model.SaveTagCommand
import com.appvoyager.litememo.domain.model.value.TagColor
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TagName
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.tagFixture
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SaveTagUseCaseTest {

    @Test
    fun invokeCreatesTagWithGeneratedId() = runTest {
        // Arrange
        val useCase =
            saveTagUseCase(tagIdProvider = QueueTagIdProvider(listOf(TagId("generated-tag"))))

        // Act
        val tag = useCase(SaveTagCommand(name = TagName("Work"), color = TagColor(0xFF6750A4)))

        // Assert
        assertEquals(TagId("generated-tag"), tag.id)
    }

    @Test
    fun invokeCreatesTagWithCurrentTime() = runTest {
        // Arrange
        val useCase = saveTagUseCase(timeProvider = MutableTimeProvider(TimestampMillis(2000L)))

        // Act
        val tag = useCase(SaveTagCommand(name = TagName("Work"), color = TagColor(0xFF6750A4)))

        // Assert
        assertEquals(TimestampMillis(2000L), tag.createdAt)
    }

    @Test
    fun invokePreservesCreatedAtWhenUpdatingExistingTag() = runTest {
        // Arrange
        val existing = tagFixture(id = "tag-1", createdAt = 1000L)
        val useCase = saveTagUseCase(
            tagRepository = FakeTagRepository(listOf(existing)),
            timeProvider = MutableTimeProvider(TimestampMillis(3000L))
        )

        // Act
        val tag =
            useCase(
                SaveTagCommand(
                    id = existing.id,
                    name = TagName("New"),
                    color = TagColor(0xFF006D3B)
                )
            )

        // Assert
        assertEquals(TimestampMillis(1000L), tag.createdAt)
    }

    @Test
    fun invokeThrowsWhenTagIdDoesNotExist() {
        // Arrange
        val useCase = saveTagUseCase()

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            runTest {
                useCase(
                    SaveTagCommand(
                        id = TagId("client-id"),
                        name = TagName("New"),
                        color = TagColor(0xFF006D3B)
                    )
                )
            }
        }
    }

    @Test
    fun invokeDoesNotSaveTagWhenTagIdDoesNotExist() = runTest {
        // Arrange
        val repository = FakeTagRepository()
        val useCase = saveTagUseCase(tagRepository = repository)

        // Act
        try {
            useCase(
                SaveTagCommand(
                    id = TagId("client-id"),
                    name = TagName("New"),
                    color = TagColor(0xFF006D3B)
                )
            )
        } catch (_: IllegalArgumentException) {
        }

        // Assert
        assertEquals(emptyList<Any>(), repository.savedTags)
    }

    private fun saveTagUseCase(
        tagRepository: FakeTagRepository = FakeTagRepository(),
        tagIdProvider: QueueTagIdProvider = QueueTagIdProvider(),
        timeProvider: MutableTimeProvider = MutableTimeProvider()
    ) = SaveTagUseCase(
        tagRepository = tagRepository,
        tagIdProvider = tagIdProvider,
        currentTimeProvider = timeProvider
    )

}
