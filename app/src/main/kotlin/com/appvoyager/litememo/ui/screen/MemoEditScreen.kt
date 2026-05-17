package com.appvoyager.litememo.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.appvoyager.litememo.ui.theme.LiteMemoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoEditScreen(
    uiState: MemoEditUiState,
    onTitleChanged: (String) -> Unit,
    onBodyChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBackRequest: () -> Unit,
    onDismissDiscard: () -> Unit,
    onConfirmDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackRequest) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                title = {},
                actions = {
                    if (uiState.memoId != null) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null
                            )
                        }
                    }
                    IconButton(onClick = onSave) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingContent()

            uiState.hasError -> ErrorContent(onRetry = {})

            else -> {
                val colorScheme = MaterialTheme.colorScheme
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
                    Spacer(modifier = Modifier.height(12.dp))
                    BasicTextField(
                        value = uiState.body,
                        onValueChange = onBodyChanged,
                        modifier = Modifier.fillMaxSize(),
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
            onSave = {},
            onDelete = {},
            onBackRequest = {},
            onDismissDiscard = {},
            onConfirmDiscard = {}
        )
    }
}
