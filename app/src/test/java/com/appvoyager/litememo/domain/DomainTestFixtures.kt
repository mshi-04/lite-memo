package com.appvoyager.litememo.domain

import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.MemoTitle
import com.appvoyager.litememo.domain.model.value.TagColor
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TagName
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.provider.CurrentTimeProvider
import com.appvoyager.litememo.domain.provider.MemoIdProvider
import com.appvoyager.litememo.domain.provider.TagIdProvider
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

fun memoFixture(
    id: String = "memo-1",
    title: String = "Title",
    body: String = "Body",
    createdAt: Long = 1000L,
    updatedAt: Long = createdAt,
    tagIds: List<TagId> = emptyList(),
    isImportant: Boolean = false
) = Memo(
    id = MemoId(id),
    title = MemoTitle(title),
    body = MemoBody(body),
    createdAt = TimestampMillis(createdAt),
    updatedAt = TimestampMillis(updatedAt),
    tagIds = tagIds,
    isImportant = isImportant
)

fun tagFixture(
    id: String = "tag-1",
    name: String = "Tag",
    color: Long = 0xFF6750A4,
    createdAt: Long = 1000L
) = Tag(
    id = TagId(id),
    name = TagName(name),
    color = TagColor(color),
    createdAt = TimestampMillis(createdAt)
)

class FakeMemoRepository(initialMemos: List<Memo> = emptyList()) : MemoRepository {

    private val memos = MutableStateFlow(initialMemos)
    val savedMemos = mutableListOf<Memo>()
    val deletedIds = mutableListOf<MemoId>()

    override fun observeMemos(): Flow<List<Memo>> = memos

    override suspend fun getMemo(id: MemoId): Memo? = memos.value.firstOrNull { it.id == id }

    override suspend fun saveMemo(memo: Memo) {
        savedMemos += memo
        memos.value = memos.value.filterNot { it.id == memo.id } + memo
    }

    override suspend fun deleteMemo(id: MemoId) {
        deletedIds += id
        memos.value = memos.value.filterNot { it.id == id }
    }

    fun currentMemos(): List<Memo> = memos.value
}

class FakeTagRepository(
    initialTags: List<Tag> = emptyList(),
    private val memoRepository: FakeMemoRepository? = null
) : TagRepository {

    private val tags = MutableStateFlow(initialTags)
    val savedTags = mutableListOf<Tag>()
    val deletedIds = mutableListOf<TagId>()

    override fun observeTags(): Flow<List<Tag>> = tags

    override suspend fun getTag(id: TagId): Tag? = tags.value.firstOrNull { it.id == id }

    override suspend fun getTagsByIds(ids: List<TagId>): List<Tag> =
        tags.value.filter { it.id in ids }

    override suspend fun saveTag(tag: Tag) {
        savedTags += tag
        tags.value = tags.value.filterNot { it.id == tag.id } + tag
    }

    override suspend fun deleteTag(id: TagId) {
        deletedIds += id
        tags.value = tags.value.filterNot { it.id == id }
    }

    override suspend fun deleteTagWithMemoReferences(id: TagId) {
        deleteTag(id)
        memoRepository?.currentMemos()
            ?.filter { memo -> id in memo.tagIds }
            ?.forEach { memo ->
                memoRepository.saveMemo(memo.copy(tagIds = memo.tagIds.filterNot { it == id }))
            }
    }

    fun currentTags(): List<Tag> = tags.value
}

class QueueMemoIdProvider(ids: List<MemoId> = listOf(MemoId("memo-1"))) : MemoIdProvider {

    private val memoIds = ids.toMutableList()

    override fun newMemoId(): MemoId = memoIds.removeAt(0)
}

class QueueTagIdProvider(ids: List<TagId> = listOf(TagId("tag-1"))) : TagIdProvider {

    private val tagIds = ids.toMutableList()

    override fun newTagId(): TagId = tagIds.removeAt(0)
}

class MutableTimeProvider(var current: TimestampMillis = TimestampMillis(1000L)) :
    CurrentTimeProvider {

    override fun now(): TimestampMillis = current

}
