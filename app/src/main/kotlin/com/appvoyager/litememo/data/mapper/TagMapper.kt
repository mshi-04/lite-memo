package com.appvoyager.litememo.data.mapper

import com.appvoyager.litememo.data.local.entity.TagEntity
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.TagColor
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TagName
import com.appvoyager.litememo.domain.model.value.TimestampMillis

fun Tag.toEntity() = TagEntity(
    id = id.value,
    name = name.value,
    colorArgb = color.argb,
    createdAt = createdAt.value
)

fun TagEntity.toDomain() = Tag(
    id = TagId(id),
    name = TagName(name),
    color = TagColor(colorArgb),
    createdAt = TimestampMillis(createdAt)
)
