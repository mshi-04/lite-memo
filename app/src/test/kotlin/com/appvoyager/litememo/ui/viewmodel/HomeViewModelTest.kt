package com.appvoyager.litememo.ui.viewmodel

import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.FakeTagRepository
import com.appvoyager.litememo.domain.MutableTimeProvider
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.tagFixture
import com.appvoyager.litememo.domain.usecase.FilterMemosUseCase
import com.appvoyager.litememo.domain.usecase.GetHomeSummaryUseCase
import com.appvoyager.litememo.domain.usecase.ObserveMemosUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTagsUseCase
import com.appvoyager.litememo.ui.state.HomeFilterUiState
import java.time.Instant
import java.time.ZoneId
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var dispatcher: TestDispatcher
    private val today = Instant.parse("2026-05-11T12:00:00Z").toEpochMilli()

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
    fun uiStateReflectsObservedMemos() = runTest(dispatcher) {
        // Arrange
        val tagId = TagId("tag-1")
        val viewModel = homeViewModel(
            memos = listOf(
                memoFixture(
                    id = "memo-1",
                    title = "買い物リスト",
                    tagIds = listOf(tagId),
                    updatedAt = today
                )
            ),
            tags = listOf(tagFixture(id = tagId.value, name = "生活"))
        )

        // Act
        advanceUntilIdle()
        val state = viewModel.uiState.first { !it.isLoading }

        // Assert
        assertEquals("買い物リスト", state.memos.single().title)
    }

    @Test
    fun uiStateContainsSummaryFromAllMemos() = runTest(dispatcher) {
        // Arrange
        val viewModel = homeViewModel(
            memos = listOf(
                memoFixture(id = "normal", updatedAt = today),
                memoFixture(id = "important", updatedAt = today, isImportant = true)
            )
        )

        // Act
        advanceUntilIdle()
        val state = viewModel.uiState.first { !it.isLoading }

        // Assert
        assertEquals(2, state.summary.totalCount)
    }

    @Test
    fun selectFilterShowsOnlyImportantMemosWhenFilterIsImportant() = runTest(dispatcher) {
        // Arrange
        val viewModel = homeViewModel(
            memos = listOf(
                memoFixture(id = "normal", title = "Normal"),
                memoFixture(id = "important", title = "Important", isImportant = true)
            )
        )
        advanceUntilIdle()

        // Act
        viewModel.selectFilter(HomeFilterUiState.Important)
        advanceUntilIdle()
        val state = viewModel.uiState.first { it.selectedFilter == HomeFilterUiState.Important }

        // Assert
        assertEquals(listOf("Important"), state.memos.map { it.title })
    }

    @Test
    fun uiStateIsEmptyWhenNoMemosExist() = runTest(dispatcher) {
        // Arrange
        val viewModel = homeViewModel()

        // Act
        advanceUntilIdle()
        val state = viewModel.uiState.first { !it.isLoading }

        // Assert
        assertEquals(emptyList<MemoId>(), state.memos.map { it.id })
    }

    @Test
    fun uiStateKeepsErrorCauseWhenObserveMemosFails() = runTest(dispatcher) {
        // Arrange
        val expected = IllegalStateException("Failed to observe memos.")
        val viewModel = homeViewModel(memoRepository = FailingMemoRepository(expected))

        // Act
        advanceUntilIdle()
        val state = viewModel.uiState.first { it.hasError }

        // Assert
        assertTrue(state.hasError)
    }

    private fun homeViewModel(
        memos: List<Memo> = emptyList(),
        tags: List<Tag> = emptyList(),
        memoRepository: MemoRepository = FakeMemoRepository(memos)
    ): HomeViewModel {
        val tagRepository = FakeTagRepository(tags)
        return HomeViewModel(
            observeMemosUseCase = ObserveMemosUseCase(memoRepository),
            observeTagsUseCase = ObserveTagsUseCase(tagRepository),
            filterMemosUseCase = FilterMemosUseCase(),
            getHomeSummaryUseCase = GetHomeSummaryUseCase(
                currentTimeProvider = MutableTimeProvider(TimestampMillis(today)),
                zoneId = ZoneId.of("UTC")
            )
        )
    }

    private class FailingMemoRepository(private val throwable: Throwable) : MemoRepository {

        override fun observeMemos(): Flow<List<Memo>> = flow {
            throw throwable
        }

        override fun observeMemos(from: TimestampMillis, to: TimestampMillis): Flow<List<Memo>> =
            flow { throw throwable }

        override suspend fun getMemo(id: MemoId): Memo? = null

        override suspend fun saveMemo(memo: Memo) = Unit

        override suspend fun deleteMemo(id: MemoId) = Unit
    }
}
