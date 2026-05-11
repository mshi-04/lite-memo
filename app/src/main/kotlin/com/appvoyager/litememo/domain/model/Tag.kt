package com.appvoyager.litememo.domain.model

import com.appvoyager.litememo.domain.model.value.TagColor
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TagName
import com.appvoyager.litememo.domain.model.value.TimestampMillis

data class Tag(
    val id: TagId,
    val name: TagName,
    val color: TagColor,
    val createdAt: TimestampMillis
)
