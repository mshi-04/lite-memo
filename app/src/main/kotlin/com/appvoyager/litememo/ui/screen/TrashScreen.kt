package com.appvoyager.litememo.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.appvoyager.litememo.R
import com.appvoyager.litememo.ui.component.ErrorContent
import com.appvoyager.litememo.ui.component.LoadingContent
import com.appvoyager.litememo.ui.component.MessageContent
import com.appvoyager.litememo.ui.state.TagUiModel
import com.appvoyager.litememo.ui.state.TrashUiState
import com.appvoyager.litememo.ui.state.TrashedMemoUiModel
import com.appvoyager.litememo.ui.theme.LiteMemoTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    uiState: TrashUiState,
    onBackClick: () -> Unit,
    onRestoreClick: (String) -> Unit,
    onPermanentDeleteRequest: (TrashedMemoUiModel) -> Unit,
    onConfirmPermanentDelete: () -> Unit,
    onDismissPermanentDelete: () -> Unit,
    onDismissActionError: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                },
                title = { Text(text = stringResource(R.string.trash_title)) }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingContent(modifier = Modifier.padding(innerPadding))

            uiState.hasError -> ErrorContent(
                onRetry = onRetry,
                modifier = Modifier.padding(innerPadding)
            )

            uiState.memos.isEmpty() -> MessageContent(
                title = stringResource(R.string.trash_empty_title),
                body = stringResource(R.string.trash_empty_body),
                modifier = Modifier.padding(innerPadding)
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.memos,
                    key = { it.id }
                ) { memo ->
                    TrashedMemoCard(
                        memo = memo,
                        onRestoreClick = { onRestoreClick(memo.id) },
                        onPermanentDeleteClick = { onPermanentDeleteRequest(memo) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    uiState.showPermanentDeleteDialog?.let { memo ->
        AlertDialog(
            onDismissRequest = onDismissPermanentDelete,
            title = { Text(text = stringResource(R.string.trash_permanent_delete_title)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.trash_permanent_delete_message,
                        memo.title
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmPermanentDelete) {
                    Text(
                        text = stringResource(R.string.trash_permanent_delete_label),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissPermanentDelete) {
                    Text(text = stringResource(R.string.cancel_label))
                }
            }
        )
    }

    if (uiState.hasActionError) {
        AlertDialog(
            onDismissRequest = onDismissActionError,
            title = { Text(text = stringResource(R.string.trash_action_error_title)) },
            text = { Text(text = stringResource(R.string.trash_action_error_body)) },
            confirmButton = {
                TextButton(onClick = onDismissActionError) {
                    Text(text = stringResource(R.string.settings_dialog_ok))
                }
            }
        )
    }
}

@Composable
private fun TrashedMemoCard(
    memo: TrashedMemoUiModel,
    onRestoreClick: () -> Unit,
    onPermanentDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = memo.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = memo.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (memo.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    memo.tags.forEach { tag ->
                        TrashTag(tag = tag)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        R.string.trash_deleted_at_label,
                        deletedAtLabel(memo.deletedAtMillis)
                    ),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium
                )
                TextButton(onClick = onRestoreClick) {
                    Text(text = stringResource(R.string.trash_restore_label))
                }
                IconButton(onClick = onPermanentDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.trash_permanent_delete_label),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun TrashTag(tag: TagUiModel?) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (tag != null) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(tag.colorArgb.toInt()))
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = tag?.name ?: stringResource(R.string.unorganized_label),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun deletedAtLabel(deletedAtMillis: Long): String {
    val zoneId = remember { ZoneId.systemDefault() }
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy/M/d H:mm") }
    val deletedAt = remember(deletedAtMillis, zoneId) {
        Instant.ofEpochMilli(deletedAtMillis).atZone(zoneId)
    }
    return formatter.format(deletedAt)
}

@Preview(showBackground = true)
@Composable
private fun TrashScreenPreview() {
    LiteMemoTheme {
        TrashScreen(
            uiState = TrashUiState(
                isLoading = false,
                memos = listOf(
                    TrashedMemoUiModel(
                        id = "memo-1",
                        title = "買い物メモ",
                        body = "牛乳、卵、コーヒー",
                        tags = listOf(TagUiModel("tag-1", "生活", 0xFF6750A4)),
                        deletedAtMillis = 1_000L
                    )
                )
            ),
            onBackClick = {},
            onRestoreClick = {},
            onPermanentDeleteRequest = {},
            onConfirmPermanentDelete = {},
            onDismissPermanentDelete = {},
            onDismissActionError = {},
            onRetry = {}
        )
    }
}
