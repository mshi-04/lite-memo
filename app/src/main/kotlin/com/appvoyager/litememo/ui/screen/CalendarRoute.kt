package com.appvoyager.litememo.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appvoyager.litememo.ui.viewmodel.CalendarViewModel

@Composable
fun CalendarRoute(
    onMemoClick: (String) -> Unit,
    onCreateMemoClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(viewModel) {
        onDispose { viewModel.closeSearch() }
    }

    CalendarScreen(
        uiState = uiState,
        onPreviousMonth = { viewModel.previousMonth() },
        onNextMonth = { viewModel.nextMonth() },
        onDateSelected = { date -> viewModel.selectDate(date) },
        onCalendarExpandedToggle = { viewModel.toggleCalendarExpanded() },
        onDatePickerRequested = { viewModel.showDatePicker() },
        onDatePickerDismissed = { viewModel.dismissDatePicker() },
        onDatePicked = { date -> viewModel.selectDateFromPicker(date) },
        onSearchToggle = { viewModel.toggleSearch() },
        onSearchQueryChanged = { viewModel.updateSearchQuery(it) },
        onRetry = { viewModel.retry() },
        onMemoClick = onMemoClick,
        onCreateMemoClick = {
            viewModel.selectedDateMillis()?.let(onCreateMemoClick)
        },
        modifier = modifier
    )
}
