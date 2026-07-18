package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.model.Memo

internal sealed interface MemoSearchResult {

    data object Inactive : MemoSearchResult

    data class Success(val query: String, val memos: List<Memo>) : MemoSearchResult

    data class Failure(val query: String) : MemoSearchResult

}
