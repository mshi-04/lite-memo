package com.appvoyager.litememo.data.repository

import com.appvoyager.litememo.data.local.dao.TagDao
import com.appvoyager.litememo.data.local.entity.TagEntity
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.tagFixture
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RoomTagRepositoryTest {

    @Test
    fun observeTagsReturnsDomainTagsFromDao() = runTest {
        // Arrange
        val dao = FakeTagDao(tags = listOf(tagEntity(id = "tag-1")))
        val repository = RoomTagRepository(dao)

        // Act
        val tags = repository.observeTags().first()

        // Assert
        assertEquals(listOf(TagId("tag-1")), tags.map { it.id })
    }

    @Test
    fun getTagReturnsNullWhenDaoReturnsNull() = runTest {
        // Arrange
        val repository = RoomTagRepository(FakeTagDao())

        // Act
        val tag = repository.getTag(TagId("missing"))

        // Assert
        assertNull(tag)
    }

    @Test
    fun getTagsByIdsReturnsTagsInRequestedOrder() = runTest {
        // Arrange
        val dao = FakeTagDao(
            tags = listOf(
                tagEntity(id = "tag-1"),
                tagEntity(id = "tag-2")
            )
        )
        val repository = RoomTagRepository(dao)

        // Act
        val tags = repository.getTagsByIds(listOf(TagId("tag-2"), TagId("tag-1")))

        // Assert
        assertEquals(listOf(TagId("tag-2"), TagId("tag-1")), tags.map { it.id })
    }

    @Test
    fun getTagsByIdsReturnsEmptyListWhenIdsAreEmpty() = runTest {
        // Arrange
        val dao = FakeTagDao(tags = listOf(tagEntity(id = "tag-1")))
        val repository = RoomTagRepository(dao)

        // Act
        val tags = repository.getTagsByIds(emptyList())

        // Assert
        assertEquals(emptyList<Tag>(), tags)
    }

    // 空リスト時に不要なDBクエリを発行しない最適化を検証する
    @Test
    fun getTagsByIdsDoesNotCallDaoWhenIdsAreEmpty() = runTest {
        // Arrange
        val dao = FakeTagDao(tags = listOf(tagEntity(id = "tag-1")))
        val repository = RoomTagRepository(dao)

        // Act
        repository.getTagsByIds(emptyList())

        // Assert
        assertEquals(0, dao.getTagsByIdsCallCount)
    }

    @Test
    fun saveTagWritesTagEntityToDao() = runTest {
        // Arrange
        val dao = FakeTagDao()
        val repository = RoomTagRepository(dao)

        // Act
        repository.saveTag(tagFixture(id = "tag-1", name = "Work"))

        // Assert
        assertEquals("tag-1", dao.savedTag?.id)
    }

    @Test
    fun deleteTagDelegatesTagIdValueToDao() = runTest {
        // Arrange
        val dao = FakeTagDao()
        val repository = RoomTagRepository(dao)

        // Act
        repository.deleteTag(TagId("tag-1"))

        // Assert
        assertEquals("tag-1", dao.deletedTagId)
    }

    private fun tagEntity(id: String, name: String = "Tag") = TagEntity(
        id = id,
        name = name,
        colorArgb = 0xFF6750A4,
        createdAt = 1000L
    )

    private class FakeTagDao(tags: List<TagEntity> = emptyList()) : TagDao {

        private val tags = MutableStateFlow(tags)
        var getTagsByIdsCallCount = 0
        var savedTag: TagEntity? = null
        var savedTags: List<TagEntity> = emptyList()
        var deletedTagId: String? = null

        override fun observeTags(): Flow<List<TagEntity>> = tags

        override suspend fun getTag(id: String): TagEntity? = tags.value.firstOrNull { it.id == id }

        override suspend fun getTagsByIds(ids: List<String>): List<TagEntity> {
            getTagsByIdsCallCount += 1
            return tags.value.filter { it.id in ids }
        }

        override suspend fun upsertTag(tag: TagEntity) {
            savedTag = tag
        }

        override suspend fun deleteTag(id: String) {
            deletedTagId = id
        }

        override suspend fun getAllTags(): List<TagEntity> = tags.value

        override suspend fun upsertAllTags(tags: List<TagEntity>) {
            savedTags = tags
            savedTag = tags.lastOrNull()
        }
    }

}
