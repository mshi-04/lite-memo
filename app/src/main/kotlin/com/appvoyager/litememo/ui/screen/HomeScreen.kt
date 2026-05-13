package com.appvoyager.litememo.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.appvoyager.litememo.R
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.TagColor
import com.appvoyager.litememo.ui.state.HomeFilterUiState
import com.appvoyager.litememo.ui.state.HomeMemoUiModel
import com.appvoyager.litememo.ui.state.HomeSummaryUiState
import com.appvoyager.litememo.ui.state.HomeUiState
import com.appvoyager.litememo.ui.theme.LiteMemoTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onFilterSelected: (HomeFilterUiState) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            uiState.isLoading -> LoadingContent()

            uiState.hasError -> MessageContent(
                title = stringResource(R.string.unknown_error),
                body = null
            )

            else -> HomeContent(
                uiState = uiState,
                onFilterSelected = onFilterSelected
            )
        }
    }
}

@Composable
private fun HomeContent(uiState: HomeUiState, onFilterSelected: (HomeFilterUiState) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HomeTopBar()
        }
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
            HomeFilters(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = onFilterSelected
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
                MemoCard(memo = memo)
            }
        }
    }
}

@Composable
private fun HomeTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
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
private fun HomeFilters(
    selectedFilter: HomeFilterUiState,
    onFilterSelected: (HomeFilterUiState) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
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
    }
}

@Composable
private fun FilterButton(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label) }
    )
}

@Composable
private fun MemoCard(memo: HomeMemoUiModel) {
    val accentColor = memo.accentColor()

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        color = accentColor,
                        size = size.copy(width = 4.dp.toPx())
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 18.dp, top = 14.dp, end = 14.dp, bottom = 14.dp)
            ) {
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MemoTag(label = memo.tagName)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = updatedAtLabel(memo.updatedAtMillis),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoTag(label: String?) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Text(
            text = label ?: stringResource(R.string.unorganized_label),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeMemoUiModel.accentColor(): Color {
    if (isImportant) return MaterialTheme.colorScheme.error
    return tagColor?.let { Color(it.argb.toULong()) } ?: MaterialTheme.colorScheme.primary
}

@Composable
private fun updatedAtLabel(updatedAtMillis: Long): String {
    val zoneId = remember { ZoneId.systemDefault() }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("H:mm") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("M/d") }
    val updatedAt = remember(updatedAtMillis, zoneId) {
        Instant.ofEpochMilli(updatedAtMillis).atZone(zoneId)
    }
    val today = LocalDate.now(zoneId)

    return when (updatedAt.toLocalDate()) {
        today -> timeFormatter.format(updatedAt)
        today.minusDays(1) -> stringResource(R.string.yesterday_label)
        else -> dateFormatter.format(updatedAt)
    }
}

@Composable
private fun EmptyHomeContent() {
    MessageContent(
        title = stringResource(R.string.empty_home_title),
        body = stringResource(R.string.empty_home_body)
    )
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MessageContent(title: String, body: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (body != null) {
            Text(
                text = body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    LiteMemoTheme {
        HomeScreen(
            uiState = previewHomeState(),
            onFilterSelected = {}
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
            onFilterSelected = {}
        )
    }
}

private fun previewHomeState() = HomeUiState(
    isLoading = false,
    summary = HomeSummaryUiState(
        totalCount = 4,
        todayCount = 2,
        unorganizedCount = 2,
        importantCount = 1
    ),
    memos = listOf(
        HomeMemoUiModel(
            id = MemoId("memo-1"),
            title = "買い物リスト",
            body = "卵、牛乳、コーヒー豆。帰りに駅前で買う。",
            tagName = "生活",
            tagColor = TagColor(0xFF6750A4),
            updatedAtMillis = System.currentTimeMillis(),
            isImportant = false
        ),
        HomeMemoUiModel(
            id = MemoId("memo-2"),
            title = "会議メモ",
            body = "次回までに画面構成と保存方式を確認する。",
            tagName = "仕事",
            tagColor = TagColor(0xFFB3261E),
            updatedAtMillis = System.currentTimeMillis(),
            isImportant = true
        )
    )
)
