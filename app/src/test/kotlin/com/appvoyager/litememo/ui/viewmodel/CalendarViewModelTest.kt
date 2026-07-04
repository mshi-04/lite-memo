package com.appvoyager.litememo.ui.viewmodel

import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.FakeTagRepository
import com.appvoyager.litememo.domain.MutableTimeProvider
import com.appvoyager.litememo.domain.epochMillis
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.SearchQuery
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.repository.FakeUserSettingsRepository
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.tagFixture
import com.appvoyager.litememo.domain.usecase.ObserveCalendarMonthSummaryUseCase
import com.appvoyager.litememo.domain.usecase.ObserveMemosByCalendarDateUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTagsUseCase
import com.appvoyager.litememo.domain.usecase.SearchMemosUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

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
    fun selectDateFromPickerUsesUtcSoDeviceWestOfUtcKeepsSameDay() = runTest(dispatcher) {
        // Arrange
        // DatePicker は日付を UTC midnight で符号化する。端末が UTC より西でも前日にずれない。
        val viewModel = calendarViewModel(zone = ZoneId.of("America/Los_Angeles"))
        advanceUntilIdle()

        // Act
        viewModel.selectDateFromPicker(epochMillis("2026-07-03T00:00:00Z"))
        advanceUntilIdle()
        val state = viewModel.uiState.first { !it.isLoading }

        // Assert
        assertEquals(LocalDate.of(2026, 7, 3), state.selectedDate)
    }

    @Test
    fun selectDateFromPickerUsesUtcSoDeviceEastOfUtcDoesNotShiftToNextDay() = runTest(dispatcher) {
        // Arrange
        // UTC 日付として解釈するため、端末タイムゾーンでは翌日になる時刻でもずれない。
        val viewModel = calendarViewModel(zone = ZoneId.of("Asia/Tokyo"))
        advanceUntilIdle()

        // Act
        viewModel.selectDateFromPicker(epochMillis("2026-07-03T23:00:00Z"))
        advanceUntilIdle()
        val state = viewModel.uiState.first { !it.isLoading }

        // Assert
        assertEquals(LocalDate.of(2026, 7, 3), state.selectedDate)
    }

    @Test
    fun closeSearchClearsSearchState() = runTest(dispatcher) {
        // Arrange
        val viewModel = calendarViewModel()
        advanceUntilIdle()
        viewModel.toggleSearch()
        viewModel.updateSearchQuery("shopping")
        advanceUntilIdle()
        val activeState = viewModel.uiState.first { it.isSearchActive }
        assertEquals(true to "shopping", activeState.isSearchActive to activeState.searchQuery)

        // Act
        viewModel.closeSearch()
        advanceUntilIdle()
        val state = viewModel.uiState.first { !it.isSearchActive }

        // Assert
        assertEquals(false to "", state.isSearchActive to state.searchQuery)
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

    @Test
    fun uiStateReflectsMemoTagsForSelectedDate() = runTest(dispatcher) {
        // Arrange
        val tagId = TagId("tag-1")
        val viewModel = calendarViewModel(
            memoRepository = FakeMemoRepository(
                listOf(
                    memoFixture(
                        id = "memo-1",
                        createdAt = epochMillis("2026-05-15T10:00:00Z"),
                        tagIds = listOf(tagId)
                    )
                )
            ),
            tags = listOf(tagFixture(id = tagId.value, name = "仕事"))
        )

        // Act
        advanceUntilIdle()
        val state = viewModel.uiState.first { !it.isLoading }

        // Assert
        assertEquals(listOf("仕事"), state.memos.single().tags.map { it.name })
    }

    @Test
    fun retryReloadsSearchResultsAfterSearchError() = runTest(dispatcher) {
        // Arrange
        val memoRepository = RetryableSearchMemoRepository(
            delegate = FakeMemoRepository(
                listOf(
                    memoFixture(
                        id = "memo-1",
                        title = "Shopping",
                        body = "Buy coffee",
                        createdAt = epochMillis("2026-05-15T10:00:00Z")
                    )
                )
            )
        )
        val viewModel = calendarViewModel(memoRepository = memoRepository)
        viewModel.toggleSearch()
        viewModel.updateSearchQuery("Shopping")
        advanceUntilIdle()
        viewModel.uiState.first { it.hasSearchError }

        // Act
        memoRepository.allowSearch()
        viewModel.retry()
        advanceUntilIdle()
        val state = viewModel.uiState.first { !it.hasSearchError && it.searchResults.isNotEmpty() }

        // Assert
        assertEquals(
            false to listOf("Shopping"),
            state.hasSearchError to state.searchResults.map { it.title }
        )
    }

    private fun calendarViewModel(
        memoRepository: MemoRepository = FakeMemoRepository(),
        tags: List<Tag> = emptyList(),
        zone: ZoneId = zoneId
    ): CalendarViewModel {
        val tagRepository = FakeTagRepository(tags)
        val userSettingsRepository = FakeUserSettingsRepository()
        return CalendarViewModel(
            observeCalendarMonthSummaryUseCase = ObserveCalendarMonthSummaryUseCase(
                memoRepository = memoRepository,
                zoneId = zone
            ),
            observeMemosByCalendarDateUseCase = ObserveMemosByCalendarDateUseCase(
                memoRepository = memoRepository,
                userSettingsRepository = userSettingsRepository,
                zoneId = zone
            ),
            observeTagsUseCase = ObserveTagsUseCase(tagRepository),
            searchMemosUseCase = SearchMemosUseCase(
                memoRepository = memoRepository,
                userSettingsRepository = userSettingsRepository
            ),
            currentTimeProvider = MutableTimeProvider(TimestampMillis(today)),
            zoneId = zone
        )
    }

    private class RetryableSearchMemoRepository(private val delegate: FakeMemoRepository) :
        MemoRepository {

        private var searchFails = true

        fun allowSearch() {
            searchFails = false
        }

        override fun observeActiveMemos(): Flow<List<Memo>> = delegate.observeActiveMemos()

        override fun observeActiveMemosBySearchQuery(query: SearchQuery): Flow<List<Memo>> =
            if (searchFails) {
                flow { throw IllegalStateException("Search failed.") }
            } else {
                delegate.observeActiveMemosBySearchQuery(query)
            }

        override fun observeActiveMemosCreatedBetween(
            from: TimestampMillis,
            to: TimestampMillis
        ): Flow<List<Memo>> = delegate.observeActiveMemosCreatedBetween(from, to)

        override fun observeTrashedMemos(): Flow<List<Memo>> = delegate.observeTrashedMemos()

        override suspend fun getActiveMemo(id: MemoId): Memo? = delegate.getActiveMemo(id)

        override suspend fun saveMemo(memo: Memo) = delegate.saveMemo(memo)

        override suspend fun moveMemoToTrash(id: MemoId, deletedAt: TimestampMillis) =
            delegate.moveMemoToTrash(id, deletedAt)

        override suspend fun restoreMemoFromTrash(id: MemoId) = delegate.restoreMemoFromTrash(id)

        override suspend fun deleteMemoPermanently(id: MemoId) = delegate.deleteMemoPermanently(id)

        override suspend fun discardMemo(id: MemoId) = delegate.discardMemo(id)

        override suspend fun deleteTrashedMemosDeletedAtOrBefore(cutoff: TimestampMillis) =
            delegate.deleteTrashedMemosDeletedAtOrBefore(cutoff)

        override suspend fun getAllActiveMemos(): List<Memo> = delegate.getAllActiveMemos()

        override suspend fun saveAllMemos(memos: List<Memo>) = delegate.saveAllMemos(memos)

        override suspend fun importAll(tags: List<Tag>, memos: List<Memo>) =
            delegate.importAll(tags, memos)
    }
}
