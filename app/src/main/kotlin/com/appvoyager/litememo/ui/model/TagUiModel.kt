package com.appvoyager.litememo.ui.model

import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.TagId

data class TagUiModel(val id: TagId, val name: String, val colorArgb: Long) {

    companion object {
        fun fromDomain(tag: Tag) = TagUiModel(
            id = tag.id,
            name = tag.name.value,
            colorArgb = tag.color.argb
        )
    }

}
