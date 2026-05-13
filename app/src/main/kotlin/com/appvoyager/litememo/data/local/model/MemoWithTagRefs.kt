package com.appvoyager.litememo.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.appvoyager.litememo.data.local.entity.MemoEntity
import com.appvoyager.litememo.data.local.entity.MemoTagRefEntity

data class MemoWithTagRefs(
    @Embedded val memo: MemoEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "memoId"
    )
    val tagRefs: List<MemoTagRefEntity>
)
