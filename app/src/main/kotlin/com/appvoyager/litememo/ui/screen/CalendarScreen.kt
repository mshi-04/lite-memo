package com.appvoyager.litememo.ui.screen

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.appvoyager.litememo.R
import com.appvoyager.litememo.domain.model.CalendarDate
import com.appvoyager.litememo.domain.model.CalendarMonth
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.TagColor
import com.appvoyager.litememo.ui.state.CalendarDayUiState
import com.appvoyager.litememo.ui.state.CalendarMemoUiModel
import com.appvoyager.litememo.ui.state.CalendarUiState
import com.appvoyager.litememo.ui.theme.LiteMemoTheme
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (CalendarDate) -> Unit,
    onCalendarExpandedToggle: () -> Unit,
    onDatePickerRequested: () -> Unit,
    onDatePickerDismissed: () -> Unit,
    onDatePicked: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            uiState.isLoading -> CalendarLoadingContent()

            uiState.hasError -> CalendarMessageContent(
                title = stringResource(R.string.unknown_error),
                body = null
            )

            else -> CalendarContent(
                uiState = uiState,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onDateSelected = onDateSelected,
                onCalendarExpandedToggle = onCalendarExpandedToggle,
                onDatePickerRequested = onDatePickerRequested
            )
        }

        if (uiState.isDatePickerVisible && uiState.selectedDate != null) {
            CalendarDatePickerDialog(
                selectedDate = uiState.selectedDate,
                onDatePicked = onDatePicked,
                onDismiss = onDatePickerDismissed
            )
        }
    }
}

@Composable
private fun CalendarContent(
    uiState: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (CalendarDate) -> Unit,
    onCalendarExpandedToggle: () -> Unit,
    onDatePickerRequested: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CalendarTopBar()
        }
        item {
            CalendarMonthCard(
                uiState = uiState,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onDateSelected = onDateSelected,
                onCalendarExpandedToggle = onCalendarExpandedToggle,
                onDatePickerRequested = onDatePickerRequested
            )
        }
        item {
            Text(
                text = selectedDateTitle(uiState.selectedDate),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        if (uiState.memos.isEmpty()) {
            item {
                CalendarMessageContent(
                    title = stringResource(R.string.empty_calendar_title),
                    body = stringResource(R.string.empty_calendar_body)
                )
            }
        } else {
            items(
                items = uiState.memos,
                key = { memo -> memo.id }
            ) { memo ->
                CalendarMemoCard(memo = memo)
            }
        }
    }
}

@Composable
private fun CalendarTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.calendar_title),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CalendarMonthCard(
    uiState: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (CalendarDate) -> Unit,
    onCalendarExpandedToggle: () -> Unit,
    onDatePickerRequested: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = calendarContainerColor()),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            CalendarMonthHeader(
                month = uiState.selectedMonth,
                isExpanded = uiState.isCalendarExpanded,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onCalendarExpandedToggle = onCalendarExpandedToggle,
                onDatePickerRequested = onDatePickerRequested
            )
            AnimatedVisibility(
                visible = uiState.isCalendarExpanded,
                enter = expandVertically(
                    animationSpec = tween(220)
                ) + fadeIn(
                    animationSpec = tween(160)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(180)
                ) + fadeOut(
                    animationSpec = tween(120)
                )
            ) {
                Column(
                    modifier = Modifier.animateContentSize(animationSpec = tween(220))
                ) {
                    Spacer(modifier = Modifier.height(18.dp))
                    AnimatedCalendarGrid(
                        month = uiState.selectedMonth,
                        days = uiState.days,
                        onDateSelected = onDateSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarMonthHeader(
    month: CalendarMonth?,
    isExpanded: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCalendarExpandedToggle: () -> Unit,
    onDatePickerRequested: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = monthTitle(month),
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onDatePickerRequested),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.previous_month)
            )
        }
        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.next_month)
            )
        }
        IconButton(onClick = onCalendarExpandedToggle) {
            Icon(
                imageVector = if (isExpanded) {
                    Icons.Default.KeyboardArrowUp
                } else {
                    Icons.Default.KeyboardArrowDown
                },
                contentDescription = stringResource(R.string.toggle_calendar)
            )
        }
    }
}

@Composable
private fun AnimatedCalendarGrid(
    month: CalendarMonth?,
    days: List<CalendarDayUiState>,
    onDateSelected: (CalendarDate) -> Unit
) {
    AnimatedContent(
        targetState = CalendarGridAnimationState(
            month = month?.value,
            days = days
        ),
        transitionSpec = {
            val direction = if (targetState.month.isAfter(initialState.month)) 1 else -1
            (
                slideInHorizontally(
                    animationSpec = tween(220),
                    initialOffsetX = { width -> width / 4 * direction }
                ) + fadeIn(animationSpec = tween(160))
                ).togetherWith(
                slideOutHorizontally(
                    animationSpec = tween(220),
                    targetOffsetX = { width -> -width / 4 * direction }
                ) + fadeOut(animationSpec = tween(120))
            ).using(SizeTransform(clip = false))
        },
        label = "calendar-month-grid"
    ) { state ->
        CalendarGrid(
            days = state.days,
            onDateSelected = onDateSelected
        )
    }
}

@Composable
private fun CalendarGrid(days: List<CalendarDayUiState>, onDateSelected: (CalendarDate) -> Unit) {
    // ISO DayOfWeek: MON=1..SUN=7。% 7 で SUN=0 になり、日曜始まりの列オフセットになる
    val leadingBlankCount = days.firstOrNull()?.date?.value?.dayOfWeek?.value?.rem(DAY_COUNT) ?: 0
    val cells = List(leadingBlankCount) { null } + days

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CalendarWeekHeader()
        cells.chunked(DAY_COUNT).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(DAY_COUNT) { index ->
                    val day = week.getOrNull(index)
                    CalendarDayCell(
                        day = day,
                        onDateSelected = onDateSelected,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarWeekHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        stringArrayResource(R.array.week_day_labels).forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDayUiState?,
    onDateSelected: (CalendarDate) -> Unit,
    modifier: Modifier = Modifier
) {
    if (day == null) {
        Spacer(modifier = modifier.aspectRatio(1f))
        return
    }

    val containerColor = if (day.isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    val contentColor = if (day.isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(containerColor)
            .clickable { onDateSelected(day.date) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.dayOfMonth.toString(),
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        if (day.hasMemo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(contentColor)
            )
        }
    }
}

@Composable
private fun CalendarMemoCard(memo: CalendarMemoUiModel) {
    val accentColor = memo.accentColor()

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    CalendarMemoTag(label = memo.tagName)
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
private fun CalendarMemoTag(label: String?) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarDatePickerDialog(
    selectedDate: CalendarDate,
    onDatePicked: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedDateMillis = remember(selectedDate) {
        selectedDate.value.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    datePickerState.selectedDateMillis?.let(onDatePicked) ?: onDismiss()
                }
            ) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun CalendarLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CalendarMessageContent(title: String, body: String?) {
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

@Composable
private fun CalendarMemoUiModel.accentColor(): Color {
    if (isImportant) return MaterialTheme.colorScheme.error
    return tagColor?.let { Color(it.argb.toInt()) } ?: MaterialTheme.colorScheme.primary
}

@Composable
private fun calendarContainerColor(): Color {
    if (isSystemInDarkTheme()) {
        return MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
    }

    return Color.White
}

@Composable
private fun selectedDateTitle(date: CalendarDate?): String {
    val pattern = stringResource(R.string.selected_date_title_format)
    val formatter = remember(pattern) { DateTimeFormatter.ofPattern(pattern) }
    return date?.value?.format(formatter) ?: ""
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
private fun monthTitle(month: CalendarMonth?): String {
    val pattern = stringResource(R.string.month_title_format)
    val formatter = remember(pattern) { DateTimeFormatter.ofPattern(pattern) }
    return month?.value?.format(formatter) ?: ""
}

private const val DAY_COUNT = 7

private data class CalendarGridAnimationState(
    val month: YearMonth?,
    val days: List<CalendarDayUiState>
)

private fun YearMonth?.isAfter(other: YearMonth?): Boolean {
    if (this == null || other == null) return true
    return this > other
}

@Preview(showBackground = true)
@Composable
private fun CalendarScreenPreview() {
    LiteMemoTheme {
        CalendarScreen(
            uiState = previewCalendarState(),
            onPreviousMonth = {},
            onNextMonth = {},
            onDateSelected = {},
            onCalendarExpandedToggle = {},
            onDatePickerRequested = {},
            onDatePickerDismissed = {},
            onDatePicked = {}
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun CalendarScreenDarkPreview() {
    LiteMemoTheme {
        CalendarScreen(
            uiState = previewCalendarState(),
            onPreviousMonth = {},
            onNextMonth = {},
            onDateSelected = {},
            onCalendarExpandedToggle = {},
            onDatePickerRequested = {},
            onDatePickerDismissed = {},
            onDatePicked = {}
        )
    }
}

private fun previewCalendarState(): CalendarUiState {
    val month = CalendarMonth(YearMonth.of(2026, 5))
    val selectedDate = CalendarDate(LocalDate.of(2026, 5, 15))
    return CalendarUiState(
        isLoading = false,
        selectedMonth = month,
        selectedDate = selectedDate,
        days = month.toCalendarDates().map { date ->
            CalendarDayUiState(
                date = date,
                dayOfMonth = date.value.dayOfMonth,
                isSelected = date == selectedDate,
                hasMemo = date.value.dayOfMonth in listOf(9, 11, 14, 15, 19, 28)
            )
        },
        memos = listOf(
            CalendarMemoUiModel(
                id = MemoId("memo-1"),
                title = "週次レビュー",
                body = "完了したタスクと来週の優先度を整理する。",
                tagName = "仕事",
                tagColor = TagColor(0xFF6750A4),
                updatedAtMillis = System.currentTimeMillis(),
                isImportant = false
            ),
            CalendarMemoUiModel(
                id = MemoId("memo-2"),
                title = "献立メモ",
                body = "冷蔵庫の野菜を使い切る。買い足しは卵。",
                tagName = "生活",
                tagColor = TagColor(0xFF006D3B),
                updatedAtMillis = System.currentTimeMillis(),
                isImportant = false
            )
        )
    )
}
