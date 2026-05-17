package com.appvoyager.litememo.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appvoyager.litememo.ui.viewmodel.CalendarViewModel

@Composable
fun CalendarRoute(modifier: Modifier = Modifier, viewModel: CalendarViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CalendarScreen(
        uiState = uiState,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onDateSelected = viewModel::selectDate,
        onCalendarExpandedToggle = viewModel::toggleCalendarExpanded,
        onDatePickerRequested = viewModel::showDatePicker,
        onDatePickerDismissed = viewModel::dismissDatePicker,
        onDatePicked = viewModel::selectDateFromPicker,
        onRetry = viewModel::retry,
        modifier = modifier
    )
}
