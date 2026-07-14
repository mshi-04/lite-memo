package com.appvoyager.litememo.domain.model

import com.appvoyager.litememo.domain.model.value.TagId

sealed class MemoFilter {
    data object All : MemoFilter()
    data object Unorganized : MemoFilter()
    data object Favorite : MemoFilter()
    data class ByTag(val tagId: TagId) : MemoFilter()
}
