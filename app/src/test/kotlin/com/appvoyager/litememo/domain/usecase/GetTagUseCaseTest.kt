package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.FakeTagRepository
import com.appvoyager.litememo.domain.tagFixture
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetTagUseCaseTest {

    @Test
    fun invokeReturnsTagById() = runBlocking {
        // Arrange
        val tag = tagFixture(id = "tag-1")
        val repository = FakeTagRepository(listOf(tag))

        // Act
        val result = GetTagUseCase(repository)(tag.id)

        // Assert
        assertEquals(tag, result)
    }

}
