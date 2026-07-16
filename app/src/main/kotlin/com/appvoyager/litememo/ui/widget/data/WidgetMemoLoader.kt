package com.appvoyager.litememo.ui.widget.data

import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.usecase.ObserveMemosUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class WidgetMemoLoader(private val observeMemosUseCase: ObserveMemosUseCase) {

    suspend fun loadRecent(limit: Int): List<WidgetItem> = observeMemosUseCase().first()
        .take(limit)
        .map { it.toWidgetItem() }

    fun observeRecent(limit: Int): Flow<List<WidgetItem>> = observeMemosUseCase()
        .map { memos -> memos.take(limit).map { it.toWidgetItem() } }

}

private const val MAX_TITLE_LENGTH = 50
private const val MAX_SNIPPET_LENGTH = 80

private fun Memo.toWidgetItem(): WidgetItem {
    val trimmedTitle = title.value.trim()
    val bodyLines = body.value.trim().lines().map { it.trim() }.filter { it.isNotEmpty() }
    val primary: String
    val snippet: String
    if (trimmedTitle.isNotEmpty()) {
        primary = trimmedTitle
        snippet = bodyLines.joinToString(" ")
    } else {
        primary = bodyLines.firstOrNull().orEmpty()
        snippet = bodyLines.drop(1).joinToString(" ")
    }
    return WidgetItem(
        id = id,
        title = primary.take(MAX_TITLE_LENGTH),
        snippet = snippet.take(MAX_SNIPPET_LENGTH),
        isFavorite = isFavorite
    )
}
