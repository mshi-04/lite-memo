package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.model.Memo

sealed interface MemoSearchUiResult {

    data object Inactive : MemoSearchUiResult

    data class Success(val query: String, val memos: List<Memo>) : MemoSearchUiResult

    data class Failure(val query: String) : MemoSearchUiResult

}
