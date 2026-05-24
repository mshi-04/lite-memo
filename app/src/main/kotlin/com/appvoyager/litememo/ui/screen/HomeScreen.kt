package com.appvoyager.litememo.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.appvoyager.litememo.R
import com.appvoyager.litememo.domain.model.MemoSortOrder
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.ui.component.ErrorContent
import com.appvoyager.litememo.ui.component.LoadingContent
import com.appvoyager.litememo.ui.component.MemoCard
import com.appvoyager.litememo.ui.component.MessageContent
import com.appvoyager.litememo.ui.component.toDisplayString
import com.appvoyager.litememo.ui.state.HomeFilterUiState
import com.appvoyager.litememo.ui.state.HomeSummaryUiState
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
    onImportantToggle: (String, Boolean) -> Unit,
    onMemoClick: (String) -> Unit,
    onCreateMemoClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateMemoClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_memo))
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
                onImportantToggle = onImportantToggle,
                onMemoClick = onMemoClick,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onFilterSelected: (HomeFilterUiState) -> Unit,
    onSortOrderSelected: (MemoSortOrder) -> Unit,
    onSearchToggle: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onImportantToggle: (String, Boolean) -> Unit,
    onMemoClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HomeTopBar(
                isSearchActive = uiState.isSearchActive,
                searchQuery = uiState.searchQuery,
                onSearchToggle = onSearchToggle,
                onSearchQueryChanged = onSearchQueryChanged
            )
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
                    MemoCard(
                        memo = memo,
                        onClick = { onMemoClick(memo.id) },
                        onImportantToggle = {
                            onImportantToggle(memo.id, !memo.isImportant)
                        }
                    )
                }
            }
        } else {
            item {
                Text(
                    text = stringResource(R.string.welcome_back),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.recent_memos),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                SummaryCard(summary = uiState.summary)
            }
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
                    MemoCard(
                        memo = memo,
                        onClick = { onMemoClick(memo.id) },
                        onImportantToggle = {
                            onImportantToggle(memo.id, !memo.isImportant)
                        }
                    )
                }
            }
        }
    }
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
private fun SummaryCard(summary: HomeSummaryUiState) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.memo_count, summary.totalCount),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.home_summary_detail,
                        summary.unorganizedCount,
                        summary.importantCount
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            TodayCountBox(todayCount = summary.todayCount)
        }
    }
}

@Composable
private fun TodayCountBox(todayCount: Int) {
    Column(
        modifier = Modifier
            .size(width = 72.dp, height = 60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = todayCount.toString(),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.today_label),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
    }
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
                label = stringResource(R.string.filter_important),
                selected = selectedFilter == HomeFilterUiState.Important,
                onClick = { onFilterSelected(HomeFilterUiState.Important) }
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
                        .background(Color(it.toInt()))
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
            onImportantToggle = { _, _ -> },
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
            onImportantToggle = { _, _ -> },
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
    summary = HomeSummaryUiState(
        totalCount = 4,
        todayCount = 2,
        unorganizedCount = 2,
        importantCount = 1
    ),
    memos = listOf(
        MemoUiModel(
            id = "memo-1",
            title = "買い物リスト",
            body = "卵、牛乳、コーヒー豆。帰りに駅前で買う。",
            tags = listOf(TagUiModel("tag-life", "生活", 0xFF6750A4)),
            updatedAtMillis = System.currentTimeMillis(),
            isImportant = false
        ),
        MemoUiModel(
            id = "memo-2",
            title = "会議メモ",
            body = "次回までに画面構成と保存方式を確認する。",
            tags = listOf(TagUiModel("tag-work", "仕事", 0xFFB3261E)),
            updatedAtMillis = System.currentTimeMillis(),
            isImportant = true
        )
    )
)
