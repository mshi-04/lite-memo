package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.model.Tag

data class TagUiModel(val id: String, val name: String, val colorArgb: Long) {
    companion object {
        fun fromDomain(tag: Tag) = TagUiModel(
            id = tag.id.value,
            name = tag.name.value,
            colorArgb = tag.color.argb
        )
    }
}
