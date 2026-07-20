package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.ui.type.HomeFilterUiType

data class HomeFilterUiState(val type: HomeFilterUiType, val tagId: TagId? = null) {

    init {
        require(type != HomeFilterUiType.ByTag || tagId != null) {
            "ByTag filter requires a tagId."
        }
        require(type == HomeFilterUiType.ByTag || tagId == null) {
            "Only ByTag filter can have a tagId."
        }
    }

    companion object {
        val All = HomeFilterUiState(HomeFilterUiType.All)
        val Unorganized = HomeFilterUiState(HomeFilterUiType.Unorganized)
        val Favorite = HomeFilterUiState(HomeFilterUiType.Favorite)

        fun byTag(tagId: TagId) = HomeFilterUiState(HomeFilterUiType.ByTag, tagId)
    }
}
