package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.MemoFilter
import javax.inject.Inject

class FilterMemosUseCase @Inject constructor() {

    operator fun invoke(memos: List<Memo>, filter: MemoFilter): List<Memo> = when (filter) {
        MemoFilter.All -> memos
        MemoFilter.Unorganized -> memos.filter { it.tagIds.isEmpty() }
        MemoFilter.Favorite -> memos.filter { it.isFavorite }
        is MemoFilter.ByTag -> memos.filter { filter.tagId in it.tagIds }
    }

}
