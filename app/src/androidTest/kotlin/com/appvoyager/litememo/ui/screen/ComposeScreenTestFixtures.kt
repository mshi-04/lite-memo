package com.appvoyager.litememo.ui.screen

import androidx.compose.runtime.Composable
import com.appvoyager.litememo.ui.state.MemoImageUiModel
import com.appvoyager.litememo.ui.state.MemoUiModel
import com.appvoyager.litememo.ui.state.TagUiModel
import com.appvoyager.litememo.ui.theme.LiteMemoTheme

@Composable
internal fun TestScreenContent(content: @Composable () -> Unit) {
    LiteMemoTheme {
        content()
    }
}

internal fun testMemoUiModel(
    id: String = "memo-1",
    title: String = "Title",
    body: String = "Body",
    tags: List<TagUiModel> = emptyList(),
    thumbnailPath: String? = null
) = MemoUiModel(
    id = id,
    title = title,
    body = body,
    tags = tags,
    updatedAtMillis = 1_000L,
    isFavorite = false,
    thumbnailPath = thumbnailPath
)

internal fun testTagUiModel(id: String = "tag-1", name: String = "Tag") = TagUiModel(
    id = id,
    name = name,
    colorArgb = 0xFF4CAF50
)

internal fun testMemoImageUiModel(
    id: String = "image-1",
    fileName: String = "image-1.jpg",
    filePath: String = "/missing/image-1.jpg",
    isPersisted: Boolean = true
) = MemoImageUiModel(
    id = id,
    fileName = fileName,
    filePath = filePath,
    isPersisted = isPersisted
)
