package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.model.value.TagId

data class HomeFilterUiState(val type: Type, val tagId: TagId? = null) {

    init {
        require(type != Type.ByTag || tagId != null) {
            "ByTag filter requires a tagId."
        }
        require(type == Type.ByTag || tagId == null) {
            "Only ByTag filter can have a tagId."
        }
    }

    enum class Type {
        All,
        Unorganized,
        Favorite,
        ByTag
    }

    companion object {
        val All = HomeFilterUiState(Type.All)
        val Unorganized = HomeFilterUiState(Type.Unorganized)
        val Favorite = HomeFilterUiState(Type.Favorite)

        fun byTag(tagId: TagId) = HomeFilterUiState(Type.ByTag, tagId)
    }
}
