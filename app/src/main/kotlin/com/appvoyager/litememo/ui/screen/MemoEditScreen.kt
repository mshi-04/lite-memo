package com.appvoyager.litememo.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appvoyager.litememo.R
import com.appvoyager.litememo.ui.component.ErrorContent
import com.appvoyager.litememo.ui.component.LoadingContent
import com.appvoyager.litememo.ui.component.toComposeColor
import com.appvoyager.litememo.ui.state.MemoEditUiState
import com.appvoyager.litememo.ui.state.TagUiModel
import com.appvoyager.litememo.ui.theme.LiteMemoTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemoEditScreen(
    uiState: MemoEditUiState,
    onTitleChanged: (String) -> Unit,
    onBodyChanged: (String) -> Unit,
    onTagToggled: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBackRequest: () -> Unit,
    onRetry: () -> Unit,
    onShareMemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBackRequest) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                },
                title = {},
                actions = {
                    if (!uiState.isDeletePending) {
                        var menuExpanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.more_options)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            val hasContent = uiState.title.isNotBlank() || uiState.body.isNotBlank()
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.share_memo)) },
                                onClick = {
                                    menuExpanded = false
                                    onShareMemo()
                                },
                                enabled = hasContent
                            )
                        }
                    }
                    if (uiState.memoId != null && !uiState.isDeletePending) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete_memo)
                            )
                        }
                    }
                    if (!uiState.isDeletePending) {
                        IconButton(onClick = onSave) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.save_memo),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingContent()

            uiState.hasError -> ErrorContent(onRetry = onRetry)

            else -> {
                val colorScheme = MaterialTheme.colorScheme
                val bodyFocusRequester = remember { FocusRequester() }
                LaunchedEffect(uiState.memoId) {
                    if (uiState.memoId == null) {
                        withFrameNanos { }
                        runCatching { bodyFocusRequester.requestFocus() }
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    BasicTextField(
                        value = uiState.title,
                        onValueChange = onTitleChanged,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        ),
                        decorationBox = { innerTextField ->
                            if (uiState.title.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.memo_edit_title_hint),
                                    style = TextStyle(
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (uiState.availableTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            uiState.availableTags.forEach { tag ->
                                val selected = tag.id in uiState.selectedTagIds
                                FilterChip(
                                    selected = selected,
                                    onClick = { onTagToggled(tag.id) },
                                    label = { Text(text = tag.name) },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(tag.toComposeColor())
                                        )
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    BasicTextField(
                        value = uiState.body,
                        onValueChange = onBodyChanged,
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(bodyFocusRequester),
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = colorScheme.onSurface
                        ),
                        decorationBox = { innerTextField ->
                            if (uiState.body.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.memo_edit_body_hint),
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MemoEditScreenPreview() {
    LiteMemoTheme {
        MemoEditScreen(
            uiState = MemoEditUiState(
                isLoading = false,
                memoId = "memo-1",
                title = "買い物リスト",
                body = "卵、牛乳、コーヒー豆。帰りに駅前で買う。",
                isModified = true
            ),
            onTitleChanged = {},
            onBodyChanged = {},
            onTagToggled = {},
            onSave = {},
            onDelete = {},
            onBackRequest = {},
            onRetry = {},
            onShareMemo = {}
        )
    }
}
