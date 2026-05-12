package com.appvoyager.litememo.domain.model

import com.appvoyager.litememo.domain.model.value.TagColor
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TagName

data class SaveTagCommand(val id: TagId? = null, val name: TagName, val color: TagColor)
