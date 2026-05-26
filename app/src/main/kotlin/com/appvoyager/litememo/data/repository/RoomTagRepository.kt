package com.appvoyager.litememo.data.repository

import com.appvoyager.litememo.data.local.dao.TagDao
import com.appvoyager.litememo.data.mapper.toDomain
import com.appvoyager.litememo.data.mapper.toEntity
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.repository.TagRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTagRepository @Inject constructor(private val tagDao: TagDao) : TagRepository {

    override fun observeTags(): Flow<List<Tag>> =
        tagDao.observeTags().map { tags -> tags.map { it.toDomain() } }

    override suspend fun getTag(id: TagId): Tag? = tagDao.getTag(id.value)?.toDomain()

    override suspend fun getTagsByIds(ids: List<TagId>): List<Tag> {
        if (ids.isEmpty()) return emptyList()

        val tagsById = tagDao.getTagsByIds(ids.map { it.value })
            .associateBy { it.id }
        return ids.mapNotNull { id -> tagsById[id.value]?.toDomain() }
    }

    override suspend fun saveTag(tag: Tag) {
        tagDao.upsertTag(tag.toEntity())
    }

    override suspend fun deleteTag(id: TagId) {
        tagDao.deleteTag(id.value)
    }

    override suspend fun getAllTags(): List<Tag> =
        TODO("Implement in feature/export-import-data branch")

    override suspend fun saveAllTags(tags: List<Tag>) {
        TODO("Implement in feature/export-import-data branch")
    }

}
