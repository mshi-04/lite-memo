package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.FakeTagRepository
import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.value.TagId
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

    @Test
    fun invokeRemovesDeletedTagIdFromTaggedMemo() = runBlocking {
        // Arrange
        val targetTagId = TagId("target")
        val memo = memoFixture(id = "memo-1", tagIds = listOf(targetTagId, TagId("other")))
        val memoRepository = FakeMemoRepository(listOf(memo))
        val tagRepository = FakeTagRepository(
            initialTags = listOf(tagFixture(id = targetTagId.value)),
            memoRepository = memoRepository
        )

        // Act
        DeleteTagUseCase(tagRepository)(targetTagId)

        // Assert
        assertEquals(listOf(TagId("other")), memoRepository.getMemo(memo.id)?.tagIds)
    }

}
