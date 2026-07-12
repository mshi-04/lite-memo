package com.appvoyager.litememo.ui.widget.common

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.appvoyager.litememo.MainActivity
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.ui.navigation.WidgetNavRequest
import com.appvoyager.litememo.ui.navigation.encodeNavigationArgument

object WidgetLaunchIntents {
    const val ACTION_WIDGET_OPEN = "com.appvoyager.litememo.action.WIDGET_OPEN"
    const val EXTRA_TARGET = "widget_target"
    const val EXTRA_MEMO_ID = "widget_memo_id"
    const val TARGET_NEW_MEMO = "new_memo"
    const val TARGET_OPEN_MEMO = "open_memo"

    fun newMemoIntent(context: Context): Intent = Intent(context, MainActivity::class.java).apply {
        action = ACTION_WIDGET_OPEN
        putExtra(EXTRA_TARGET, TARGET_NEW_MEMO)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun openMemoIntent(context: Context, memoId: MemoId): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = ACTION_WIDGET_OPEN
            putExtra(EXTRA_TARGET, TARGET_OPEN_MEMO)
            putExtra(EXTRA_MEMO_ID, memoId.value)
            // リスト各行の PendingIntent は extra だけ異なると衝突するため、
            // memoId を含む一意な data を付けて filterEquals を分離する。
            data = "litememo://memo/${encodeNavigationArgument(memoId.value)}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun parseWidgetNav(action: String?, target: String?, memoId: String?): WidgetNavRequest? {
        if (action != ACTION_WIDGET_OPEN) return null
        return when (target) {
            TARGET_NEW_MEMO -> WidgetNavRequest.NewMemo
            TARGET_OPEN_MEMO -> memoId?.let(::memoIdOrNull)?.let(WidgetNavRequest::OpenMemo)
            else -> null
        }
    }

    private fun memoIdOrNull(rawValue: String): MemoId? =
        runCatching { MemoId(rawValue) }.getOrNull()
}
