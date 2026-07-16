package com.appvoyager.litememo.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.ui.event.PreviewTrashScreenActions
import com.appvoyager.litememo.ui.model.TagUiModel
import com.appvoyager.litememo.ui.model.TrashedMemoUiModel
import com.appvoyager.litememo.ui.state.TrashUiState
import com.appvoyager.litememo.ui.theme.LiteMemoTheme

private const val PREVIEW_TAG_COLOR = 0xFF6750A4
private const val PREVIEW_DELETED_AT = 1_000L

@Preview(showBackground = true, name = "ゴミ箱一覧")
@Composable
private fun TrashScreenPreview() {
    LiteMemoTheme {
        TrashScreen(
            uiState = TrashUiState(
                isLoading = false,
                memos = listOf(
                    TrashedMemoUiModel(
                        id = MemoId("memo-1"),
                        title = "買い物メモ",
                        body = "牛乳、卵、コーヒー",
                        tags = listOf(TagUiModel("tag-1", "生活", PREVIEW_TAG_COLOR)),
                        deletedAt = TimestampMillis(PREVIEW_DELETED_AT)
                    )
                )
            ),
            actions = PreviewTrashScreenActions
        )
    }
}

@Preview(showBackground = true, name = "空状態")
@Composable
private fun TrashScreenEmptyPreview() {
    LiteMemoTheme {
        TrashScreen(
            uiState = TrashUiState(isLoading = false),
            actions = PreviewTrashScreenActions
        )
    }
}

@Preview(showBackground = true, name = "読み込み中")
@Composable
private fun TrashScreenLoadingPreview() {
    LiteMemoTheme {
        TrashScreen(
            uiState = TrashUiState(isLoading = true),
            actions = PreviewTrashScreenActions
        )
    }
}

@Preview(showBackground = true, name = "エラー")
@Composable
private fun TrashScreenErrorPreview() {
    LiteMemoTheme {
        TrashScreen(
            uiState = TrashUiState(isLoading = false, hasError = true),
            actions = PreviewTrashScreenActions
        )
    }
}
