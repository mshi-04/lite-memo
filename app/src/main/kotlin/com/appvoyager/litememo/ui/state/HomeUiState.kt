package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.ui.model.MemoUiModel
import com.appvoyager.litememo.ui.model.TagUiModel

data class HomeUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val selectedFilter: HomeFilterUiState = HomeFilterUiState.All,
    val search: SearchUiState = SearchUiState(),
    val selection: HomeSelectionUiState = HomeSelectionUiState(),
    val allSelectedFavorite: Boolean = false,
    val allSelectedTagIds: Set<TagId> = emptySet(),
    val bulkTagDialog: HomeBulkTagDialogUiState = HomeBulkTagDialogUiState(),
    val tags: List<TagUiModel> = emptyList(),
    val memos: List<MemoUiModel> = emptyList()
)

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

enum class HomeFilterUiType {
    All,
    Unorganized,
    Favorite,
    ByTag
}

data class HomeSelectionUiState(val selectedMemoIds: Set<MemoId> = emptySet()) {
    val isActive: Boolean
        get() = selectedMemoIds.isNotEmpty()

    val selectedCount: Int
        get() = selectedMemoIds.size

    fun contains(memoId: MemoId): Boolean = memoId in selectedMemoIds
}

data class HomeBulkTagDialogUiState(val isVisible: Boolean = false)
