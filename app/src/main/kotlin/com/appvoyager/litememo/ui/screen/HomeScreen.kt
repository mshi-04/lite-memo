package com.appvoyager.litememo.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.appvoyager.litememo.R
import com.appvoyager.litememo.domain.model.MemoSortOrder
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.ui.component.ErrorContent
import com.appvoyager.litememo.ui.component.LoadingContent
import com.appvoyager.litememo.ui.component.MemoCard
import com.appvoyager.litememo.ui.component.MessageContent
import com.appvoyager.litememo.ui.component.tagColor
import com.appvoyager.litememo.ui.component.toComposeColor
import com.appvoyager.litememo.ui.component.toDisplayString
import com.appvoyager.litememo.ui.state.HomeBulkTagDialogUiState
import com.appvoyager.litememo.ui.state.HomeFilterUiState
import com.appvoyager.litememo.ui.state.HomeUiState
import com.appvoyager.litememo.ui.state.MemoUiModel
import com.appvoyager.litememo.ui.state.TagUiModel
import com.appvoyager.litememo.ui.theme.LiteMemoTheme

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onFilterSelected: (HomeFilterUiState) -> Unit,
    onSortOrderSelected: (MemoSortOrder) -> Unit,
    onSearchToggle: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onMemoLongClick: (String) -> Unit,
    onMemoSelectionToggle: (String) -> Unit,
    onClearSelection: () -> Unit,
    onMoveSelectedMemosToTrash: () -> Unit,
    onSetSelectedMemosFavorite: (Boolean) -> Unit,
    onRequestAddTagToSelectedMemos: () -> Unit,
    onRequestRemoveTagFromSelectedMemos: () -> Unit,
    onApplySelectedTag: (TagId) -> Unit,
    onDismissBulkTagDialog: () -> Unit,
    onShareSelectedMemo: () -> Unit,
    onMemoClick: (String) -> Unit,
    onCreateMemoClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (!uiState.selection.isActive) {
                FloatingActionButton(onClick = onCreateMemoClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.create_memo)
                    )
                }
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingContent()

            uiState.hasError -> ErrorContent(onRetry = onRetry)

            else -> HomeContent(
                uiState = uiState,
                onFilterSelected = onFilterSelected,
                onSortOrderSelected = onSortOrderSelected,
                onSearchToggle = onSearchToggle,
                onSearchQueryChanged = onSearchQueryChanged,
                onMemoLongClick = onMemoLongClick,
                onMemoSelectionToggle = onMemoSelectionToggle,
                onClearSelection = onClearSelection,
                onMoveSelectedMemosToTrash = onMoveSelectedMemosToTrash,
                onSetSelectedMemosFavorite = onSetSelectedMemosFavorite,
                onRequestAddTagToSelectedMemos = onRequestAddTagToSelectedMemos,
                onRequestRemoveTagFromSelectedMemos = onRequestRemoveTagFromSelectedMemos,
                onShareSelectedMemo = onShareSelectedMemo,
                onMemoClick = onMemoClick,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    HomeBulkTagDialog(
        uiState = uiState,
        onApplySelectedTag = onApplySelectedTag,
        onDismiss = onDismissBulkTagDialog
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onFilterSelected: (HomeFilterUiState) -> Unit,
    onSortOrderSelected: (MemoSortOrder) -> Unit,
    onSearchToggle: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onMemoLongClick: (String) -> Unit,
    onMemoSelectionToggle: (String) -> Unit,
    onClearSelection: () -> Unit,
    onMoveSelectedMemosToTrash: () -> Unit,
    onSetSelectedMemosFavorite: (Boolean) -> Unit,
    onRequestAddTagToSelectedMemos: () -> Unit,
    onRequestRemoveTagFromSelectedMemos: () -> Unit,
    onShareSelectedMemo: () -> Unit,
    onMemoClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allSelectedFavorite = (uiState.memos + uiState.searchResults)
        .filter { uiState.selection.contains(MemoId(it.id)) }
        .let { selected -> selected.isNotEmpty() && selected.all { it.isFavorite } }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            if (uiState.selection.isActive) {
                HomeSelectionToolbar(
                    selectedCount = uiState.selection.selectedCount,
                    allSelectedFavorite = allSelectedFavorite,
                    onClearSelection = onClearSelection,
                    onMoveToTrash = onMoveSelectedMemosToTrash,
                    onToggleFavorite = { onSetSelectedMemosFavorite(!allSelectedFavorite) },
                    onAddTag = onRequestAddTagToSelectedMemos,
                    onRemoveTag = onRequestRemoveTagFromSelectedMemos,
                    onShareSelectedMemo = onShareSelectedMemo
                )
            } else {
                HomeTopBar(
                    isSearchActive = uiState.isSearchActive,
                    searchQuery = uiState.searchQuery,
                    onSearchToggle = onSearchToggle,
                    onSearchQueryChanged = onSearchQueryChanged
                )
            }
        }
        if (uiState.isSearchActive) {
            when {
                uiState.hasSearchError -> item {
                    MessageContent(
                        title = stringResource(R.string.search_error_title),
                        body = stringResource(R.string.search_error_body)
                    )
                }

                uiState.searchQuery.isBlank() -> item {
                    MessageContent(
                        title = stringResource(R.string.search_hint),
                        body = stringResource(R.string.search_hint_body)
                    )
                }

                uiState.searchResults.isEmpty() -> item {
                    MessageContent(
                        title = stringResource(R.string.no_search_results_title),
                        body = stringResource(R.string.no_search_results_body)
                    )
                }

                else -> items(
                    items = uiState.searchResults,
                    key = { memo -> memo.id }
                ) { memo ->
                    SelectableMemoCard(
                        memo = memo,
                        isSelectionActive = uiState.selection.isActive,
                        selected = uiState.selection.contains(MemoId(memo.id)),
                        onMemoClick = onMemoClick,
                        onMemoSelectionToggle = onMemoSelectionToggle,
                        onMemoLongClick = onMemoLongClick
                    )
                }
            }
        } else {
            item {
                HomeFilterAndSortRow(
                    selectedFilter = uiState.selectedFilter,
                    tags = uiState.tags,
                    onFilterSelected = onFilterSelected,
                    currentSortOrder = uiState.memoSortOrder,
                    onSortOrderSelected = onSortOrderSelected
                )
            }
            if (uiState.memos.isEmpty()) {
                item {
                    EmptyHomeContent()
                }
            } else {
                items(
                    items = uiState.memos,
                    key = { memo -> memo.id }
                ) { memo ->
                    SelectableMemoCard(
                        memo = memo,
                        isSelectionActive = uiState.selection.isActive,
                        selected = uiState.selection.contains(MemoId(memo.id)),
                        onMemoClick = onMemoClick,
                        onMemoSelectionToggle = onMemoSelectionToggle,
                        onMemoLongClick = onMemoLongClick
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectableMemoCard(
    memo: MemoUiModel,
    isSelectionActive: Boolean,
    selected: Boolean,
    onMemoClick: (String) -> Unit,
    onMemoSelectionToggle: (String) -> Unit,
    onMemoLongClick: (String) -> Unit
) {
    MemoCard(
        memo = memo,
        onClick = {
            if (isSelectionActive) {
                onMemoSelectionToggle(memo.id)
            } else {
                onMemoClick(memo.id)
            }
        },
        onLongClick = { onMemoLongClick(memo.id) },
        selected = selected
    )
}

@Composable
private fun HomeTopBar(
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchToggle: () -> Unit,
    onSearchQueryChanged: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSearchActive) {
            IconButton(onClick = onSearchToggle) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close_search)
                )
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = { Text(text = stringResource(R.string.search_hint)) },
                singleLine = true,
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.clear_search),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    null
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                shape = RoundedCornerShape(12.dp)
            )
        } else {
            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onSearchToggle) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search)
                )
            }
        }
    }
}

@Composable
private fun HomeSelectionToolbar(
    selectedCount: Int,
    allSelectedFavorite: Boolean,
    onClearSelection: () -> Unit,
    onMoveToTrash: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: () -> Unit,
    onShareSelectedMemo: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClearSelection) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.clear_selection)
                )
            }
            Text(
                text = stringResource(R.string.selected_memo_count, selectedCount),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onMoveToTrash) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.move_selected_memos_to_trash),
                    tint = MaterialTheme.colorScheme.error
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (allSelectedFavorite) {
                        Icons.Default.Star
                    } else {
                        Icons.Default.StarBorder
                    },
                    contentDescription = if (allSelectedFavorite) {
                        stringResource(R.string.remove_selected_memos_from_favorite)
                    } else {
                        stringResource(R.string.add_selected_memos_to_favorite)
                    }
                )
            }
            SelectionMoreMenu(
                enabled = selectedCount == 1,
                onShareClick = onShareSelectedMemo
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextButton(onClick = onAddTag) {
                Text(text = stringResource(R.string.add_tag_to_selected_memos))
            }
            TextButton(onClick = onRemoveTag) {
                Text(text = stringResource(R.string.remove_tag_from_selected_memos))
            }
        }
    }
}

@Composable
private fun SelectionMoreMenu(enabled: Boolean, onShareClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(enabled) {
        if (!enabled) expanded = false
    }
    Box {
        IconButton(
            onClick = { expanded = true },
            enabled = enabled
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.more_options)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.share_memo)) },
                onClick = {
                    expanded = false
                    onShareClick()
                }
            )
        }
    }
}

@Composable
private fun HomeBulkTagDialog(
    uiState: HomeUiState,
    onApplySelectedTag: (TagId) -> Unit,
    onDismiss: () -> Unit
) {
    val operation = uiState.bulkTagDialog.operation ?: return
    val titleResId = when (operation) {
        HomeBulkTagDialogUiState.Operation.AddTag -> R.string.home_bulk_add_tag_title
        HomeBulkTagDialogUiState.Operation.RemoveTag -> R.string.home_bulk_remove_tag_title
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(titleResId)) },
        text = {
            if (uiState.tags.isEmpty()) {
                Text(text = stringResource(R.string.home_bulk_tag_empty_body))
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(
                        items = uiState.tags,
                        key = { tag -> tag.id }
                    ) { tag ->
                        TextButton(
                            onClick = { onApplySelectedTag(TagId(tag.id)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(tag.toComposeColor())
                                )
                                Text(
                                    text = tag.name,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 8.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel_label))
            }
        }
    )
}

@Composable
private fun HomeFilterAndSortRow(
    selectedFilter: HomeFilterUiState,
    tags: List<TagUiModel>,
    onFilterSelected: (HomeFilterUiState) -> Unit,
    currentSortOrder: MemoSortOrder,
    onSortOrderSelected: (MemoSortOrder) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterButton(
                label = stringResource(R.string.filter_all),
                selected = selectedFilter == HomeFilterUiState.All,
                onClick = { onFilterSelected(HomeFilterUiState.All) }
            )
            FilterButton(
                label = stringResource(R.string.unorganized_label),
                selected = selectedFilter == HomeFilterUiState.Unorganized,
                onClick = { onFilterSelected(HomeFilterUiState.Unorganized) }
            )
            FilterButton(
                label = stringResource(R.string.filter_favorite),
                selected = selectedFilter == HomeFilterUiState.Favorite,
                onClick = { onFilterSelected(HomeFilterUiState.Favorite) }
            )
            tags.forEach { tag ->
                val tagFilter = HomeFilterUiState.byTag(TagId(tag.id))
                FilterButton(
                    label = tag.name,
                    selected = selectedFilter == tagFilter,
                    onClick = { onFilterSelected(tagFilter) },
                    colorArgb = tag.colorArgb
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SortOrderDropdown(
                currentOrder = currentSortOrder,
                onSelected = onSortOrderSelected
            )
        }
    }
}

@Composable
private fun SortOrderDropdown(currentOrder: MemoSortOrder, onSelected: (MemoSortOrder) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = currentOrder.toDisplayString(),
                style = MaterialTheme.typography.labelMedium
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            MemoSortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(text = order.toDisplayString()) },
                    onClick = {
                        onSelected(order)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FilterButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    colorArgb: Long? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label) },
        leadingIcon = colorArgb?.let {
            {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(tagColor(it))
                )
            }
        }
    )
}

@Composable
private fun EmptyHomeContent() {
    MessageContent(
        title = stringResource(R.string.empty_home_title),
        body = stringResource(R.string.empty_home_body)
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    LiteMemoTheme {
        HomeScreen(
            uiState = previewHomeState(),
            onFilterSelected = {},
            onSortOrderSelected = {},
            onSearchToggle = {},
            onSearchQueryChanged = {},
            onMemoLongClick = {},
            onMemoSelectionToggle = {},
            onClearSelection = {},
            onMoveSelectedMemosToTrash = {},
            onSetSelectedMemosFavorite = {},
            onRequestAddTagToSelectedMemos = {},
            onRequestRemoveTagFromSelectedMemos = {},
            onApplySelectedTag = {},
            onDismissBulkTagDialog = {},
            onShareSelectedMemo = {},
            onMemoClick = {},
            onCreateMemoClick = {},
            onRetry = {}
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun HomeScreenDarkPreview() {
    LiteMemoTheme {
        HomeScreen(
            uiState = previewHomeState(),
            onFilterSelected = {},
            onSortOrderSelected = {},
            onSearchToggle = {},
            onSearchQueryChanged = {},
            onMemoLongClick = {},
            onMemoSelectionToggle = {},
            onClearSelection = {},
            onMoveSelectedMemosToTrash = {},
            onSetSelectedMemosFavorite = {},
            onRequestAddTagToSelectedMemos = {},
            onRequestRemoveTagFromSelectedMemos = {},
            onApplySelectedTag = {},
            onDismissBulkTagDialog = {},
            onShareSelectedMemo = {},
            onMemoClick = {},
            onCreateMemoClick = {},
            onRetry = {}
        )
    }
}

private fun previewHomeState() = HomeUiState(
    isLoading = false,
    tags = listOf(
        TagUiModel("tag-life", "生活", 0xFF6750A4),
        TagUiModel("tag-work", "仕事", 0xFFB3261E)
    ),
    memos = listOf(
        MemoUiModel(
            id = "memo-1",
            title = "買い物リスト",
            body = "卵、牛乳、コーヒー豆。帰りに駅前で買う。",
            tags = listOf(TagUiModel("tag-life", "生活", 0xFF6750A4)),
            updatedAtMillis = System.currentTimeMillis(),
            isFavorite = false
        ),
        MemoUiModel(
            id = "memo-2",
            title = "会議メモ",
            body = "次回までに画面構成と保存方式を確認する。",
            tags = listOf(TagUiModel("tag-work", "仕事", 0xFFB3261E)),
            updatedAtMillis = System.currentTimeMillis(),
            isFavorite = true
        )
    )
)
