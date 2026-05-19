package com.appvoyager.litememo.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appvoyager.litememo.R
import com.appvoyager.litememo.ui.component.ErrorContent
import com.appvoyager.litememo.ui.component.LoadingContent
import com.appvoyager.litememo.ui.state.MemoEditUiState
import com.appvoyager.litememo.ui.state.TagUiModel
import com.appvoyager.litememo.ui.theme.LiteMemoTheme
import kotlinx.coroutines.android.awaitFrame

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
    onDismissDiscard: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onRetry: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
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
                LaunchedEffect(Unit) {
                    awaitFrame()
                    bodyFocusRequester.requestFocus()
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
                                                .background(Color(tag.colorArgb.toInt()))
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

    if (uiState.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = onDismissDiscard,
            title = { Text(stringResource(R.string.discard_dialog_title)) },
            text = { Text(stringResource(R.string.discard_dialog_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmDiscard) {
                    Text(
                        stringResource(R.string.discard_label),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDiscard) {
                    Text(stringResource(R.string.cancel_label))
                }
            }
        )
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
            onDismissDiscard = {},
            onConfirmDiscard = {},
            onRetry = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
