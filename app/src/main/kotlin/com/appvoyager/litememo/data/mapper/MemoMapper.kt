package com.appvoyager.litememo.data.mapper

import com.appvoyager.litememo.data.local.entity.MemoEntity
import com.appvoyager.litememo.data.local.entity.MemoTagRefEntity
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.MemoTitle
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis

fun Memo.toEntity() = MemoEntity(
    id = id.value,
    title = title.value,
    body = body.value,
    createdAt = createdAt.value,
    updatedAt = updatedAt.value,
    isImportant = isImportant
)

fun Memo.toTagRefs() = tagIds.mapIndexed { index, tagId ->
    MemoTagRefEntity(
        memoId = id.value,
        tagId = tagId.value,
        position = index
    )
}

fun MemoEntity.toDomain(tagRefs: List<MemoTagRefEntity>) = Memo(
    id = MemoId(id),
    title = MemoTitle(title),
    body = MemoBody(body),
    createdAt = TimestampMillis(createdAt),
    updatedAt = TimestampMillis(updatedAt),
    tagIds = tagRefs.sortedBy { it.position }.map { TagId(it.tagId) },
    isImportant = isImportant
)
