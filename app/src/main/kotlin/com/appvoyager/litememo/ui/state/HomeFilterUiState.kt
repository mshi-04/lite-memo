package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.ui.type.HomeFilterType

data class HomeFilterUiState(val type: HomeFilterType, val tagId: TagId? = null) {

    init {
        require(type != HomeFilterType.ByTag || tagId != null) {
            "ByTag filter requires a tagId."
        }
        require(type == HomeFilterType.ByTag || tagId == null) {
            "Only ByTag filter can have a tagId."
        }
    }

    companion object {
        val All = HomeFilterUiState(HomeFilterType.All)
        val Unorganized = HomeFilterUiState(HomeFilterType.Unorganized)
        val Favorite = HomeFilterUiState(HomeFilterType.Favorite)

        fun byTag(tagId: TagId) = HomeFilterUiState(HomeFilterType.ByTag, tagId)
    }
}
