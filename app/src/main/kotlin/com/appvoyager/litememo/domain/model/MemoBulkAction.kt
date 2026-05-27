package com.appvoyager.litememo.domain.model

import com.appvoyager.litememo.domain.model.value.TagId

data class MemoBulkAction(
    val type: Type,
    val isFavorite: Boolean? = null,
    val tagId: TagId? = null
) {
    init {
        require(type != Type.SetFavorite || isFavorite != null) {
            "SetFavorite action requires isFavorite."
        }
        require(type == Type.SetFavorite || isFavorite == null) {
            "Only SetFavorite action can have isFavorite."
        }
        require((type == Type.AddTag || type == Type.RemoveTag) == (tagId != null)) {
            "Tag actions require tagId."
        }
    }

    enum class Type {
        MoveToTrash,
        SetFavorite,
        AddTag,
        RemoveTag
    }

    companion object {
        fun moveToTrash() = MemoBulkAction(type = Type.MoveToTrash)

        fun setFavorite(isFavorite: Boolean) = MemoBulkAction(
            type = Type.SetFavorite,
            isFavorite = isFavorite
        )

        fun addTag(tagId: TagId) = MemoBulkAction(
            type = Type.AddTag,
            tagId = tagId
        )

        fun removeTag(tagId: TagId) = MemoBulkAction(
            type = Type.RemoveTag,
            tagId = tagId
        )
    }
}
