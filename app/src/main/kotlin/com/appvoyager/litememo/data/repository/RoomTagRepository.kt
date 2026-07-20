package com.appvoyager.litememo.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.appvoyager.litememo.data.local.dao.TagDao
import com.appvoyager.litememo.data.mapper.toDomain
import com.appvoyager.litememo.data.mapper.toEntity
import com.appvoyager.litememo.domain.exception.DuplicateTagNameException
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TagName
import com.appvoyager.litememo.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomTagRepository @Inject constructor(private val tagDao: TagDao) : TagRepository {

    override fun observeTags(): Flow<List<Tag>> =
        tagDao.observeTags().map { tags -> tags.map { it.toDomain() } }

    override suspend fun getTag(id: TagId): Tag? = tagDao.getTag(id.value)?.toDomain()

    override suspend fun findTagByName(name: TagName): Tag? =
        tagDao.findTagByName(name.value)?.toDomain()

    override suspend fun getTagsByIds(ids: List<TagId>): List<Tag> {
        if (ids.isEmpty()) return emptyList()

        val tagsById = tagDao.getTagsByIds(ids.map { it.value })
            .associateBy { it.id }
        return ids.mapNotNull { id -> tagsById[id.value]?.toDomain() }
    }

    // tags の制約は主キーと name の unique index だけで、主キーは insert / update の分岐が解決する。
    // ここでの constraint 違反は name 衝突とみなし、Domain error へ変換する。
    override suspend fun saveTag(tag: Tag) {
        try {
            tagDao.insertOrUpdateAllTags(listOf(tag.toEntity()))
        } catch (_: SQLiteConstraintException) {
            throw DuplicateTagNameException(tag.name)
        }
    }

    override suspend fun deleteTag(id: TagId) {
        tagDao.deleteTag(id.value)
    }

    override suspend fun getAllTags(): List<Tag> = tagDao.getAllTags().map { it.toDomain() }

}
