package com.appvoyager.litememo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "memo_tag_refs",
    primaryKeys = ["memoId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = MemoEntity::class,
            parentColumns = ["id"],
            childColumns = ["memoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"]
        )
    ],
    indices = [
        Index(value = ["memoId"]),
        Index(value = ["tagId"])
    ]
)
data class MemoTagRefEntity(
    val memoId: String,
    val tagId: String,
    val position: Int
)
