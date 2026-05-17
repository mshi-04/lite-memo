package com.appvoyager.litememo.ui.screen

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.appvoyager.litememo.R
import com.appvoyager.litememo.ui.component.AnimatedCalendarGrid
import com.appvoyager.litememo.ui.component.ErrorContent
import com.appvoyager.litememo.ui.component.LoadingContent
import com.appvoyager.litememo.ui.component.MemoCard
import com.appvoyager.litememo.ui.component.MessageContent
import com.appvoyager.litememo.ui.state.CalendarDayUiState
import com.appvoyager.litememo.ui.state.CalendarUiState
import com.appvoyager.litememo.ui.state.MemoUiModel
import com.appvoyager.litememo.ui.theme.LiteMemoTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onCalendarExpandedToggle: () -> Unit,
    onDatePickerRequested: () -> Unit,
    onDatePickerDismissed: () -> Unit,
    onDatePicked: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            uiState.isLoading -> LoadingContent()

            uiState.hasError -> ErrorContent(onRetry = onRetry)

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
    onDateSelected: (LocalDate) -> Unit,
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
                MessageContent(
                    title = stringResource(R.string.empty_calendar_title),
                    body = stringResource(R.string.empty_calendar_body)
                )
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
    onDateSelected: (LocalDate) -> Unit,
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
    month: YearMonth?,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarDatePickerDialog(
    selectedDate: LocalDate,
    onDatePicked: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedDateMillis = remember(selectedDate) {
        selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
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
private fun calendarContainerColor(): Color {
    if (isSystemInDarkTheme()) {
        return MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
    }

    return Color.White
}

@Composable
private fun selectedDateTitle(date: LocalDate?): String {
    val pattern = stringResource(R.string.selected_date_title_format)
    val formatter = remember(pattern) { DateTimeFormatter.ofPattern(pattern) }
    return date?.format(formatter) ?: ""
}

@Composable
private fun monthTitle(month: YearMonth?): String {
    val pattern = stringResource(R.string.month_title_format)
    val formatter = remember(pattern) { DateTimeFormatter.ofPattern(pattern) }
    return month?.format(formatter) ?: ""
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
            onDatePicked = {},
            onRetry = {}
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
            onDatePicked = {},
            onRetry = {}
        )
    }
}

private fun previewCalendarState(): CalendarUiState {
    val month = YearMonth.of(2026, 5)
    val selectedDate = LocalDate.of(2026, 5, 15)
    return CalendarUiState(
        isLoading = false,
        selectedMonth = month,
        selectedDate = selectedDate,
        days = (1..month.lengthOfMonth()).map { dayOfMonth ->
            val date = month.atDay(dayOfMonth)
            CalendarDayUiState(
                date = date,
                dayOfMonth = dayOfMonth,
                isSelected = date == selectedDate,
                hasMemo = dayOfMonth in listOf(9, 11, 14, 15, 19, 28)
            )
        },
        memos = listOf(
            MemoUiModel(
                id = "memo-1",
                title = "週次レビュー",
                body = "完了したタスクと来週の優先度を整理する。",
                tagName = "仕事",
                tagColorArgb = 0xFF6750A4,
                updatedAtMillis = System.currentTimeMillis(),
                isImportant = false
            ),
            MemoUiModel(
                id = "memo-2",
                title = "献立メモ",
                body = "冷蔵庫の野菜を使い切る。買い足しは卵。",
                tagName = "生活",
                tagColorArgb = 0xFF006D3B,
                updatedAtMillis = System.currentTimeMillis(),
                isImportant = false
            )
        )
    )
}
