package com.appvoyager.litememo.data.mapper

import com.appvoyager.litememo.data.local.entity.MemoEntity
import com.appvoyager.litememo.data.local.entity.MemoTagRefEntity
import com.appvoyager.litememo.data.local.model.MemoWithTagRefs
import com.appvoyager.litememo.domain.model.value.TagId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MemoMapperTest {

    @Test
    fun toDomainReturnsTagIdsOrderedByPosition() {
        // Arrange
        val memoWithTagRefs = MemoWithTagRefs(
            memo = MemoEntity(
                id = "memo-1",
                title = "Title",
                body = "Body",
                createdAt = 1000L,
                updatedAt = 1000L,
                isImportant = false
            ),
            tagRefs = listOf(
                MemoTagRefEntity(memoId = "memo-1", tagId = "tag-2", position = 1),
                MemoTagRefEntity(memoId = "memo-1", tagId = "tag-1", position = 0)
            )
        )

        // Act
        val memo = memoWithTagRefs.toDomain()

        // Assert
        assertEquals(listOf(TagId("tag-1"), TagId("tag-2")), memo.tagIds)
    }

}
