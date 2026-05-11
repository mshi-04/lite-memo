package com.appvoyager.litememo.domain.model

import com.appvoyager.litememo.domain.model.value.TagColor
import com.appvoyager.litememo.domain.model.value.TagId

data class SaveTagCommand(
    val id: TagId? = null,
    val name: String,
    val color: TagColor
)
