package com.appvoyager.litememo.data.mapper

import com.appvoyager.litememo.data.local.entity.MemoEntity
import com.appvoyager.litememo.data.local.entity.MemoTagRefEntity
import com.appvoyager.litememo.data.local.model.MemoWithTagRefs
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.MemoTitle
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MemoMapperTest {

    @Test
    fun toEntityReturnsMemoEntityWithDomainValues() {
        // Arrange
        val memo = memoFixture(
            id = "memo-1",
            title = "Title",
            body = "Body",
            createdAt = 1000L,
            updatedAt = 2000L,
            isImportant = true
        )

        // Act
        val entity = memo.toEntity()

        // Assert
        assertEquals(
            MemoEntity(
                id = "memo-1",
                title = "Title",
                body = "Body",
                createdAt = 1000L,
                updatedAt = 2000L,
                isImportant = true
            ),
            entity
        )
    }

    @Test
    fun toTagRefsReturnsRefsOrderedByTagIds() {
        // Arrange
        val memo = memoFixture(
            id = "memo-1",
            tagIds = listOf(TagId("tag-1"), TagId("tag-2"))
        )

        // Act
        val tagRefs = memo.toTagRefs()

        // Assert
        assertEquals(
            listOf(
                MemoTagRefEntity(memoId = "memo-1", tagId = "tag-1", position = 0),
                MemoTagRefEntity(memoId = "memo-1", tagId = "tag-2", position = 1)
            ),
            tagRefs
        )
    }

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

    @Test
    fun toDomainReturnsMemoWithEntityValues() {
        // Arrange
        val entity = MemoEntity(
            id = "memo-1",
            title = "Title",
            body = "Body",
            createdAt = 1000L,
            updatedAt = 2000L,
            isImportant = true
        )

        // Act
        val memo = entity.toDomain(emptyList())

        // Assert
        assertEquals(
            Memo(
                id = MemoId("memo-1"),
                title = MemoTitle("Title"),
                body = MemoBody("Body"),
                createdAt = TimestampMillis(1000L),
                updatedAt = TimestampMillis(2000L),
                tagIds = emptyList(),
                isImportant = true
            ),
            memo
        )
    }

    @Test
    fun toDomainThrowsWhenTagRefReferencesAnotherMemo() {
        // Arrange
        val entity = MemoEntity(
            id = "memo-1",
            title = "Title",
            body = "Body",
            createdAt = 1000L,
            updatedAt = 2000L,
            isImportant = false
        )
        val tagRefs = listOf(
            MemoTagRefEntity(memoId = "memo-2", tagId = "tag-1", position = 0)
        )

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            entity.toDomain(tagRefs)
        }
    }

    @Test
    fun toDomainThrowsWhenCreatedAtIsNegative() {
        // Arrange
        val entity = MemoEntity(
            id = "memo-1",
            title = "Title",
            body = "Body",
            createdAt = -1L,
            updatedAt = 2000L,
            isImportant = false
        )

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            entity.toDomain(emptyList())
        }
    }

}
