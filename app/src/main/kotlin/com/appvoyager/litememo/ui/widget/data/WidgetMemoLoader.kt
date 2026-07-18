package com.appvoyager.litememo.ui.widget.data

import com.appvoyager.litememo.domain.model.MemoSummary
import com.appvoyager.litememo.domain.usecase.ObserveRecentMemosUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class WidgetMemoLoader(private val observeRecentMemosUseCase: ObserveRecentMemosUseCase) {

    fun observeRecent(): Flow<List<WidgetItem>> = observeRecentMemosUseCase(RECENT_MEMOS_LIMIT)
        .map { memos -> memos.map { it.toWidgetItem() } }

    suspend fun loadRecent(): List<WidgetItem> = observeRecent().first()

}

private const val RECENT_MEMOS_LIMIT = 8
private const val MAX_TITLE_LENGTH = 50
private const val MAX_SNIPPET_LENGTH = 80

// title/snippet に必要な可視長を十分カバーする本文プレフィクス。
// 巨大な本文全体を lines() 走査しないためのガード。
private const val BODY_SCAN_PREFIX = (MAX_TITLE_LENGTH + MAX_SNIPPET_LENGTH) * 4

private fun MemoSummary.toWidgetItem(): WidgetItem {
    val trimmedTitle = title.value.trim()
    val bodyLines = body.value.take(BODY_SCAN_PREFIX).trim().lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    val primary: String
    val snippet: String
    if (trimmedTitle.isNotEmpty()) {
        primary = trimmedTitle.takeCodePointSafe(MAX_TITLE_LENGTH)
        snippet = bodyLines.joinToString(" ")
    } else {
        val firstLine = bodyLines.firstOrNull().orEmpty()
        primary = firstLine.takeCodePointSafe(MAX_TITLE_LENGTH)
        // 先頭行のうちタイトル表示分より後ろ + 残りの行を snippet に回し、
        // タイトルなし1行メモで可視範囲外の内容が消えないようにする。
        val firstLineRemainder = firstLine.removePrefix(primary).trim()
        snippet = (listOf(firstLineRemainder) + bodyLines.drop(1))
            .filter { it.isNotEmpty() }
            .joinToString(" ")
    }
    return WidgetItem(
        id = id,
        title = primary.takeCodePointSafe(MAX_TITLE_LENGTH),
        snippet = snippet.takeCodePointSafe(MAX_SNIPPET_LENGTH),
        isFavorite = isFavorite
    )
}

/**
 * [maxChars] 文字までに切り詰めるが、末尾でサロゲートペアを分断しない。
 * 切り取り位置の直前が高位サロゲート単独になる場合は 1 文字手前で切る。
 */
private fun String.takeCodePointSafe(maxChars: Int): String {
    if (maxChars <= 0) return ""
    if (length <= maxChars) return this
    val lastIndex = maxChars - 1
    val end = if (this[lastIndex].isHighSurrogate()) lastIndex else maxChars
    return substring(0, end)
}
