package com.appvoyager.litememo.domain.model

import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.MemoTitle
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis

data class Memo(
    val id: MemoId,
    val title: MemoTitle,
    val body: MemoBody,
    val createdAt: TimestampMillis,
    val updatedAt: TimestampMillis,
    val tagIds: List<TagId> = emptyList(),
    val images: List<MemoImage> = emptyList(),
    val isFavorite: Boolean = false,
    val deletedAt: TimestampMillis? = null
) {

    init {
        require(updatedAt.value >= createdAt.value) {
            "Memo updatedAt must be greater than or equal to createdAt."
        }
        require(deletedAt == null || deletedAt.value >= updatedAt.value) {
            "Memo deletedAt must be greater than or equal to updatedAt."
        }
        require(images.distinctBy { it.id }.size == images.size) {
            "Memo images must not contain duplicated ids."
        }
    }

}

/**
 * updatedAt が createdAt や現在の updatedAt を下回らないよう、now をクランプして返す。
 * 端末時刻の巻き戻りなどで updatedAt が後退するのを防ぐ。
 */
fun Memo.updatedAtFrom(now: TimestampMillis): TimestampMillis =
    TimestampMillis(maxOf(now.value, updatedAt.value, createdAt.value))
