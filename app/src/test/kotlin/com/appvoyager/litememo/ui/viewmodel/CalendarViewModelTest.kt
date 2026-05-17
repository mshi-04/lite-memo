package com.appvoyager.litememo.ui.viewmodel

import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.FakeTagRepository
import com.appvoyager.litememo.domain.MutableTimeProvider
import com.appvoyager.litememo.domain.epochMillis
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.usecase.ObserveCalendarMonthSummaryUseCase
import com.appvoyager.litememo.domain.usecase.ObserveMemosByCalendarDateUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTagsUseCase
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private lateinit var dispatcher: TestDispatcher
    private val zoneId = ZoneId.of("UTC")
    private val today = epochMillis("2026-05-15T12:00:00Z")

    @BeforeEach
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun nextMonthUpdatesSelectedMonth() = runTest(dispatcher) {
        // Arrange
        val viewModel = calendarViewModel()
        advanceUntilIdle()

        // Act
        viewModel.nextMonth()
        advanceUntilIdle()
        val state = viewModel.uiState.first { !it.isLoading }

        // Assert
        assertEquals(YearMonth.of(2026, 6), state.selectedMonth)
    }

    @Test
    fun selectDateUpdatesSelectedDate() = runTest(dispatcher) {
        // Arrange
        val viewModel = calendarViewModel()
        advanceUntilIdle()

        // Act
        viewModel.selectDate(LocalDate.of(2026, 5, 11))
        advanceUntilIdle()
        val state = viewModel.uiState.first { !it.isLoading }

        // Assert
        assertEquals(LocalDate.of(2026, 5, 11), state.selectedDate)
    }

    @Test
    fun toggleCalendarExpandedCollapsesCalendar() = runTest(dispatcher) {
        // Arrange
        val viewModel = calendarViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.first { !it.isLoading }.isCalendarExpanded)

        // Act
        viewModel.toggleCalendarExpanded()
        advanceUntilIdle()
        val state = viewModel.uiState.first { !it.isLoading }

        // Assert
        assertFalse(state.isCalendarExpanded)
    }

    @Test
    fun selectDateFromPickerUpdatesSelectedMonthAndDate() = runTest(dispatcher) {
        // Arrange
        val viewModel = calendarViewModel()
        advanceUntilIdle()

        // Act
        viewModel.selectDateFromPicker(epochMillis("2026-07-03T00:00:00Z"))
        advanceUntilIdle()
        val state = viewModel.uiState.first { !it.isLoading }

        // Assert
        assertEquals(YearMonth.of(2026, 7), state.selectedMonth)
        assertEquals(LocalDate.of(2026, 7, 3), state.selectedDate)
    }

    @Test
    fun uiStateMarksOnlyMemoDatesWithDot() = runTest(dispatcher) {
        // Arrange
        val viewModel = calendarViewModel(
            memoRepository = FakeMemoRepository(
                listOf(
                    memoFixture(
                        id = "memo-1",
                        createdAt = epochMillis("2026-05-11T10:00:00Z")
                    )
                )
            )
        )

        // Act
        advanceUntilIdle()
        val state = viewModel.uiState.first { !it.isLoading }

        // Assert
        assertEquals(
            listOf(11),
            state.days.filter { day -> day.hasMemo }.map { day -> day.dayOfMonth }
        )
    }

    private fun calendarViewModel(
        memoRepository: FakeMemoRepository = FakeMemoRepository()
    ): CalendarViewModel {
        val tagRepository = FakeTagRepository()
        return CalendarViewModel(
            observeCalendarMonthSummaryUseCase = ObserveCalendarMonthSummaryUseCase(
                memoRepository = memoRepository,
                zoneId = zoneId
            ),
            observeMemosByCalendarDateUseCase = ObserveMemosByCalendarDateUseCase(
                memoRepository = memoRepository,
                zoneId = zoneId
            ),
            observeTagsUseCase = ObserveTagsUseCase(tagRepository),
            currentTimeProvider = MutableTimeProvider(TimestampMillis(today)),
            zoneId = zoneId
        )
    }
}
