package com.appvoyager.litememo.domain

import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.MemoEditDraft
import com.appvoyager.litememo.domain.model.MemoEditDraftTarget
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.MemoTitle
import com.appvoyager.litememo.domain.model.value.SearchQuery
import com.appvoyager.litememo.domain.model.value.TagColor
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TagName
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.provider.CurrentTimeProvider
import com.appvoyager.litememo.domain.provider.MemoIdProvider
import com.appvoyager.litememo.domain.provider.TagIdProvider
import com.appvoyager.litememo.domain.repository.MemoEditDraftRepository
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.repository.TagRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

fun memoFixture(
    id: String = "memo-1",
    title: String = "Title",
    body: String = "Body",
    createdAt: Long = 1000L,
    updatedAt: Long = createdAt,
    tagIds: List<TagId> = emptyList(),
    isFavorite: Boolean = false,
    deletedAt: Long? = null
) = Memo(
    id = MemoId(id),
    title = MemoTitle(title),
    body = MemoBody(body),
    createdAt = TimestampMillis(createdAt),
    updatedAt = TimestampMillis(updatedAt),
    tagIds = tagIds,
    isFavorite = isFavorite,
    deletedAt = deletedAt?.let { TimestampMillis(it) }
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

fun epochMillis(value: String): Long = Instant.parse(value).toEpochMilli()

class FakeMemoRepository(initialMemos: List<Memo> = emptyList()) : MemoRepository {

    private val memos = MutableStateFlow(initialMemos)
    val savedMemos = mutableListOf<Memo>()
    val movedToTrash = mutableListOf<Pair<MemoId, TimestampMillis>>()
    val restoredIds = mutableListOf<MemoId>()
    val permanentlyDeletedIds = mutableListOf<MemoId>()
    val purgeCutoffs = mutableListOf<TimestampMillis>()

    override fun observeActiveMemos(): Flow<List<Memo>> =
        memos.map { list -> list.filter { it.deletedAt == null } }

    override fun observeActiveMemosBySearchQuery(query: SearchQuery): Flow<List<Memo>> =
        memos.map { list ->
            list.filter { memo ->
                val matchesQuery = memo.title.value.contains(query.value, ignoreCase = true) ||
                    memo.body.value.contains(query.value, ignoreCase = true)
                memo.deletedAt == null && matchesQuery
            }
        }

    override fun observeActiveMemosCreatedBetween(
        from: TimestampMillis,
        to: TimestampMillis
    ): Flow<List<Memo>> {
        require(from.value < to.value) { "from must be earlier than to." }
        return memos.map { list ->
            list.filter { memo ->
                memo.deletedAt == null &&
                    memo.createdAt.value >= from.value &&
                    memo.createdAt.value < to.value
            }
        }
    }

    override fun observeTrashedMemos(): Flow<List<Memo>> = memos.map { list ->
        list.filter { it.deletedAt != null }.sortedByDescending { it.deletedAt?.value }
    }

    override suspend fun getActiveMemo(id: MemoId): Memo? =
        memos.value.firstOrNull { it.id == id && it.deletedAt == null }

    override suspend fun saveMemo(memo: Memo) {
        savedMemos += memo
        memos.value = memos.value.filterNot { it.id == memo.id } + memo
    }

    override suspend fun moveMemoToTrash(id: MemoId, deletedAt: TimestampMillis) {
        movedToTrash += id to deletedAt
        memos.value = memos.value.map { memo ->
            if (memo.id == id && memo.deletedAt == null) memo.copy(deletedAt = deletedAt) else memo
        }
    }

    override suspend fun restoreMemoFromTrash(id: MemoId) {
        restoredIds += id
        memos.value = memos.value.map { memo ->
            if (memo.id == id && memo.deletedAt != null) memo.copy(deletedAt = null) else memo
        }
    }

    override suspend fun deleteMemoPermanently(id: MemoId) {
        val memo = requireNotNull(memos.value.firstOrNull { it.id == id && it.deletedAt != null }) {
            "Memo not found or not in trash: ${id.value}"
        }
        permanentlyDeletedIds += id
        memos.value = memos.value.filterNot { it.id == memo.id }
    }

    override suspend fun deleteTrashedMemosDeletedAtOrBefore(cutoff: TimestampMillis) {
        purgeCutoffs += cutoff
        memos.value = memos.value.filterNot { memo ->
            val deletedAt = memo.deletedAt ?: return@filterNot false
            deletedAt.value <= cutoff.value
        }
    }

    fun currentMemos(): List<Memo> = memos.value

}

class FakeMemoEditDraftRepository(initialDrafts: List<MemoEditDraft> = emptyList()) :
    MemoEditDraftRepository {

    private val drafts = initialDrafts.associateBy { it.target }.toMutableMap()
    val savedDrafts = mutableListOf<MemoEditDraft>()
    val clearedTargets = mutableListOf<MemoEditDraftTarget>()

    override suspend fun getDraft(target: MemoEditDraftTarget): MemoEditDraft? = drafts[target]

    override suspend fun saveDraft(draft: MemoEditDraft) {
        savedDrafts += draft
        drafts[draft.target] = draft
    }

    override suspend fun clearDraft(target: MemoEditDraftTarget) {
        clearedTargets += target
        drafts.remove(target)
    }

    fun currentDrafts(): List<MemoEditDraft> = drafts.values.toList()

}

class FakeTagRepository(initialTags: List<Tag> = emptyList()) : TagRepository {

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

    fun currentTags(): List<Tag> = tags.value

}

class QueueMemoIdProvider(ids: List<MemoId> = listOf(MemoId("memo-1"))) : MemoIdProvider {

    private val memoIds = ids.toMutableList()

    override fun newMemoId(): MemoId {
        if (memoIds.isEmpty()) {
            throw IllegalStateException("No more MemoId available in QueueMemoIdProvider.")
        }

        return memoIds.removeAt(0)
    }
}

class QueueTagIdProvider(ids: List<TagId> = listOf(TagId("tag-1"))) : TagIdProvider {

    private val tagIds = ids.toMutableList()

    override fun newTagId(): TagId {
        if (tagIds.isEmpty()) {
            throw IllegalStateException("No more TagId available in QueueTagIdProvider.")
        }

        return tagIds.removeAt(0)
    }

}

class MutableTimeProvider(var current: TimestampMillis = TimestampMillis(1000L)) :
    CurrentTimeProvider {

    override fun now(): TimestampMillis = current

}
