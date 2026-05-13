package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.FakeTagRepository
import com.appvoyager.litememo.domain.tagFixture
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeleteTagUseCaseTest {

    @Test
    fun invokeDeletesTagById() = runBlocking {
        // Arrange
        val tag = tagFixture(id = "tag-1")
        val tagRepository = FakeTagRepository(listOf(tag))

        // Act
        DeleteTagUseCase(tagRepository)(tag.id)

        // Assert
        assertEquals(listOf(tag.id), tagRepository.deletedIds)
    }

}
