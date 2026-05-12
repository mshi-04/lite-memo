package com.appvoyager.litememo.domain.repository

import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.TagId
import kotlinx.coroutines.flow.Flow

interface TagRepository {

    fun observeTags(): Flow<List<Tag>>

    suspend fun getTag(id: TagId): Tag?

    suspend fun getTagsByIds(ids: List<TagId>): List<Tag>

    suspend fun saveTag(tag: Tag)

    suspend fun deleteTag(id: TagId)

}
