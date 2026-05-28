package com.appvoyager.litememo.ui.viewmodel

import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.FakeTagRepository
import com.appvoyager.litememo.domain.MutableTimeProvider
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
import com.appvoyager.litememo.domain.usecase.ApplyMemoBulkActionUseCase
import com.appvoyager.litememo.domain.usecase.FilterMemosUseCase
import com.appvoyager.litememo.domain.usecase.FormatMemoTextUseCase
import com.appvoyager.litememo.domain.usecase.GetHomeSummaryUseCase
import com.appvoyager.litememo.domain.usecase.ObserveMemoSortOrderUseCase
import com.appvoyager.litememo.domain.usecase.ObserveMemosUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTagsUseCase
import com.appvoyager.litememo.domain.usecase.SearchMemosUseCase
import com.appvoyager.litememo.domain.usecase.SetMemoFavoriteUseCase
import com.appvoyager.litememo.domain.usecase.SetMemoSortOrderUseCase
import com.appvoyager.litememo.ui.state.HomeBulkTagDialogUiState
import com.appvoyager.litememo.ui.state.HomeFilterUiState
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
                memoFixture(id = "Favorite", updatedAt = today, isFavorite = true)
            )
        )

        // Act
        advanceUntilIdle()
        val state = viewModel.uiState.first { !it.isLoading }

        // Assert
        assertEquals(2, state.summary.totalCount)
    }

    @Test
    fun uiStateReflectsObservedTags() = runTest(dispatcher) {
        // Arrange
        val viewModel = homeViewModel(
            tags = listOf(
                tagFixture(id = "tag-1", name = "仕事"),
                tagFixture(id = "tag-2", name = "生活")
            )
        )

        // Act
        advanceUntilIdle()
        val state = viewModel.uiState.first { !it.isLoading }

        // Assert
        assertEquals(listOf("仕事", "生活"), state.tags.map { it.name })
    }

    @Test
    fun selectFilterShowsOnlyFavoriteMemosWhenFilterIsFavorite() = runTest(dispatcher) {
        // Arrange
        val viewModel = homeViewModel(
            memos = listOf(
                memoFixture(id = "normal", title = "Normal"),
                memoFixture(id = "Favorite", title = "Favorite", isFavorite = true)
            )
        )
        advanceUntilIdle()

        // Act
        viewModel.selectFilter(HomeFilterUiState.Favorite)
        advanceUntilIdle()
        val state = viewModel.uiState.first { it.selectedFilter == HomeFilterUiState.Favorite }

        // Assert
        assertEquals(listOf("Favorite"), state.memos.map { it.title })
    }

    @Test
    fun selectFilterShowsOnlyTaggedMemosWhenFilterIsByTag() = runTest(dispatcher) {
        // Arrange
        val workTagId = TagId("work")
        val lifeTagId = TagId("life")
        val viewModel = homeViewModel(
            memos = listOf(
                memoFixture(id = "work-memo", title = "Work", tagIds = listOf(workTagId)),
                memoFixture(id = "life-memo", title = "Life", tagIds = listOf(lifeTagId))
            ),
            tags = listOf(
                tagFixture(id = workTagId.value, name = "仕事"),
                tagFixture(id = lifeTagId.value, name = "生活")
            )
        )
        advanceUntilIdle()

        // Act
        viewModel.selectFilter(HomeFilterUiState.byTag(workTagId))
        advanceUntilIdle()
        val state = viewModel.uiState.first {
            it.selectedFilter == HomeFilterUiState.byTag(workTagId)
        }

        // Assert
        assertEquals(listOf("Work"), state.memos.map { it.title })
    }

    @Test
    fun updateSearchQueryShowsMatchingMemosWhenSearchIsActive() = runTest(dispatcher) {
        // Arrange
        val viewModel = homeViewModel(
            memos = listOf(
                memoFixture(id = "shopping", title = "Shopping list"),
                memoFixture(id = "meeting", title = "Meeting note")
            )
        )
        advanceUntilIdle()

        // Act
        viewModel.toggleSearch()
        viewModel.updateSearchQuery("shopping")
        advanceUntilIdle()
        val state = viewModel.uiState.first { it.isSearchActive && it.searchResults.isNotEmpty() }

        // Assert
        assertEquals(
            Triple(true, "shopping", listOf("Shopping list")),
            Triple(state.isSearchActive, state.searchQuery, state.searchResults.map { it.title })
        )
    }

    @Test
    fun toggleSearchClearsQueryWhenSearchIsClosed() = runTest(dispatcher) {
        // Arrange
        val viewModel = homeViewModel()
        advanceUntilIdle()
        viewModel.toggleSearch()
        viewModel.updateSearchQuery("shopping")
        advanceUntilIdle()
        val activeState = viewModel.uiState.first { it.isSearchActive }
        assertEquals(true to "shopping", activeState.isSearchActive to activeState.searchQuery)

        // Act
        viewModel.toggleSearch()
        advanceUntilIdle()
        val state = viewModel.uiState.first { !it.isSearchActive }

        // Assert
        assertEquals("", state.searchQuery)
    }

    @Test
    fun closeSearchClearsSearchState() = runTest(dispatcher) {
        // Arrange
        val viewModel = homeViewModel()
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
    fun setMemoFavoriteUpdatesMemoFavoriteState() = runTest(dispatcher) {
        // Arrange
        val viewModel = homeViewModel(
            memos = listOf(memoFixture(id = "memo-1", title = "Favorite"))
        )
        advanceUntilIdle()

        // Act
        viewModel.setMemoFavorite("memo-1", true)
        advanceUntilIdle()
        val state = viewModel.uiState.first { it.memos.singleOrNull()?.isFavorite == true }

        // Assert
        assertTrue(state.memos.single().isFavorite)
    }

    @Test
    fun setMemoFavoriteShowsErrorWhenFavoriteUpdateFails() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(id = "memo-1")
        val viewModel = homeViewModel(
            memoRepository = SaveFailingMemoRepository(memo)
        )
        advanceUntilIdle()

        // Act
        viewModel.setMemoFavorite("memo-1", true)
        advanceUntilIdle()
        val state = viewModel.uiState.first { it.hasActionError }

        // Assert
        assertTrue(state.hasActionError)
    }

    @Test
    fun startSelectionSelectsMemoWhenMemoIsLongPressed() = runTest(dispatcher) {
        // Arrange
        val viewModel = homeViewModel(memos = listOf(memoFixture(id = "memo-1")))
        advanceUntilIdle()

        // Act
        viewModel.startSelection(MemoId("memo-1"))
        advanceUntilIdle()
        val state = viewModel.uiState.first {
            it.selection.selectedMemoIds == setOf(MemoId("memo-1"))
        }

        // Assert
        assertEquals(setOf(MemoId("memo-1")), state.selection.selectedMemoIds)
    }

    @Test
    fun toggleMemoSelectionClearsSelectionWhenLastSelectedMemoIsToggled() = runTest(dispatcher) {
        // Arrange
        val viewModel = homeViewModel(memos = listOf(memoFixture(id = "memo-1")))
        advanceUntilIdle()
        viewModel.startSelection(MemoId("memo-1"))
        viewModel.uiState.first { it.selection.isActive }

        // Act
        viewModel.toggleMemoSelection(MemoId("memo-1"))
        advanceUntilIdle()
        val state = viewModel.uiState.first { !it.selection.isActive }

        // Assert
        assertEquals(emptySet<MemoId>(), state.selection.selectedMemoIds)
    }

    @Test
    fun moveSelectedMemosToTrashClearsSelectionWhenBulkActionSucceeds() = runTest(dispatcher) {
        // Arrange
        val viewModel = homeViewModel(memos = listOf(memoFixture(id = "memo-1")))
        advanceUntilIdle()
        viewModel.startSelection(MemoId("memo-1"))
        viewModel.uiState.first { it.selection.isActive }

        // Act
        viewModel.moveSelectedMemosToTrash()
        advanceUntilIdle()
        val state = viewModel.uiState.first { !it.selection.isActive && it.memos.isEmpty() }

        // Assert
        assertEquals(false, state.selection.isActive)
    }

    @Test
    fun setSelectedMemosFavoriteKeepsSelectionWhenBulkActionFails() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(id = "memo-1")
        val viewModel = homeViewModel(
            memoRepository = SaveFailingMemoRepository(memo)
        )
        advanceUntilIdle()
        viewModel.startSelection(MemoId("memo-1"))
        viewModel.uiState.first { it.selection.isActive }

        // Act
        viewModel.setSelectedMemosFavorite(true)
        advanceUntilIdle()
        val state = viewModel.uiState.first { it.hasActionError }

        // Assert
        val expected = true to setOf(MemoId("memo-1"))
        val actual = state.hasActionError to state.selection.selectedMemoIds
        assertEquals(expected, actual)
    }

    @Test
    fun requestAddTagToSelectedMemosShowsTagDialog() = runTest(dispatcher) {
        // Arrange
        val viewModel = homeViewModel(memos = listOf(memoFixture(id = "memo-1")))
        advanceUntilIdle()
        viewModel.startSelection(MemoId("memo-1"))
        viewModel.uiState.first { it.selection.isActive }

        // Act
        viewModel.requestAddTagToSelectedMemos()
        advanceUntilIdle()
        val state = viewModel.uiState.first {
            it.bulkTagDialog.operation == HomeBulkTagDialogUiState.Operation.AddTag
        }

        // Assert
        assertEquals(HomeBulkTagDialogUiState.Operation.AddTag, state.bulkTagDialog.operation)
    }

    @Test
    fun dismissBulkTagDialogClearsDialog() = runTest(dispatcher) {
        // Arrange
        val viewModel = homeViewModel(memos = listOf(memoFixture(id = "memo-1")))
        advanceUntilIdle()
        viewModel.startSelection(MemoId("memo-1"))
        viewModel.requestAddTagToSelectedMemos()
        viewModel.uiState.first {
            it.bulkTagDialog.operation == HomeBulkTagDialogUiState.Operation.AddTag
        }

        // Act
        viewModel.dismissBulkTagDialog()
        advanceUntilIdle()
        val state = viewModel.uiState.first {
            it.selection.isActive && it.bulkTagDialog.operation == null
        }

        // Assert
        assertEquals(null, state.bulkTagDialog.operation)
    }

    @Test
    fun applySelectedTagClearsSelectionWhenBulkActionSucceeds() = runTest(dispatcher) {
        // Arrange
        val tagId = TagId("tag-1")
        val viewModel = homeViewModel(
            memos = listOf(memoFixture(id = "memo-1")),
            tags = listOf(tagFixture(id = tagId.value))
        )
        advanceUntilIdle()
        viewModel.startSelection(MemoId("memo-1"))
        viewModel.requestAddTagToSelectedMemos()
        viewModel.uiState.first {
            it.bulkTagDialog.operation == HomeBulkTagDialogUiState.Operation.AddTag
        }

        // Act
        viewModel.applySelectedTag(tagId)
        advanceUntilIdle()
        val state = viewModel.uiState.first { !it.selection.isActive }

        // Assert
        assertEquals(false to null, state.selection.isActive to state.bulkTagDialog.operation)
    }

    @Test
    fun formatMemoTextJoinsTitleAndBodyWhenBothPresent() = runTest(dispatcher) {
        // Arrange
        val viewModel = homeViewModel()

        // Act
        val formatted = viewModel.formatMemoText(" タイトル ", " 本文 ")

        // Assert
        assertEquals("タイトル\n\n本文", formatted)
    }

    @Test
    fun formatMemoTextReturnsTitleOnlyWhenBodyIsBlank() = runTest(dispatcher) {
        // Arrange
        val viewModel = homeViewModel()

        // Act
        val formatted = viewModel.formatMemoText("タイトル", "   ")

        // Assert
        assertEquals("タイトル", formatted)
    }

    @Test
    fun formatMemoTextReturnsBodyOnlyWhenTitleIsBlank() = runTest(dispatcher) {
        // Arrange
        val viewModel = homeViewModel()

        // Act
        val formatted = viewModel.formatMemoText("   ", "本文")

        // Assert
        assertEquals("本文", formatted)
    }

    @Test
    fun formatMemoTextReturnsNullWhenTitleAndBodyAreBlank() = runTest(dispatcher) {
        // Arrange
        val viewModel = homeViewModel()

        // Act
        val formatted = viewModel.formatMemoText("   ", "")

        // Assert
        assertEquals(null, formatted)
    }

    @Test
    fun getSelectedMemoForShareReturnsMemoWhenSingleMemoIsSelected() = runTest(dispatcher) {
        // Arrange
        val viewModel = homeViewModel(
            memos = listOf(memoFixture(id = "memo-1", title = "共有対象"))
        )
        advanceUntilIdle()
        viewModel.startSelection(MemoId("memo-1"))
        viewModel.uiState.first { it.selection.selectedMemoIds == setOf(MemoId("memo-1")) }

        // Act
        val selected = viewModel.getSelectedMemoForShare()

        // Assert
        assertEquals("memo-1" to "共有対象", selected?.id to selected?.title)
    }

    @Test
    fun getSelectedMemoForShareReturnsNullWhenMultipleMemosAreSelected() = runTest(dispatcher) {
        // Arrange
        val viewModel = homeViewModel(
            memos = listOf(
                memoFixture(id = "memo-1"),
                memoFixture(id = "memo-2")
            )
        )
        advanceUntilIdle()
        viewModel.startSelection(MemoId("memo-1"))
        viewModel.toggleMemoSelection(MemoId("memo-2"))
        viewModel.uiState.first {
            it.selection.selectedMemoIds == setOf(MemoId("memo-1"), MemoId("memo-2"))
        }

        // Act
        val selected = viewModel.getSelectedMemoForShare()

        // Assert
        assertEquals(null, selected)
    }

    @Test
    fun getSelectedMemoForShareReturnsNullWhenNoMemoIsSelected() = runTest(dispatcher) {
        // Arrange
        val viewModel = homeViewModel(
            memos = listOf(memoFixture(id = "memo-1"))
        )
        advanceUntilIdle()
        viewModel.uiState.first { !it.isLoading }

        // Act
        val selected = viewModel.getSelectedMemoForShare()

        // Assert
        assertEquals(null, selected)
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
        val userSettingsRepository = FakeUserSettingsRepository()
        return HomeViewModel(
            observeMemosUseCase = ObserveMemosUseCase(memoRepository, userSettingsRepository),
            observeTagsUseCase = ObserveTagsUseCase(tagRepository),
            filterMemosUseCase = FilterMemosUseCase(),
            getHomeSummaryUseCase = GetHomeSummaryUseCase(
                currentTimeProvider = MutableTimeProvider(TimestampMillis(today)),
                zoneId = ZoneId.of("UTC")
            ),
            observeMemoSortOrderUseCase = ObserveMemoSortOrderUseCase(userSettingsRepository),
            searchMemosUseCase = SearchMemosUseCase(memoRepository, userSettingsRepository),
            setMemoFavoriteUseCase = SetMemoFavoriteUseCase(
                memoRepository,
                MutableTimeProvider(TimestampMillis(today + 1))
            ),
            setMemoSortOrderUseCase = SetMemoSortOrderUseCase(userSettingsRepository),
            applyMemoBulkActionUseCase = ApplyMemoBulkActionUseCase(
                memoRepository = memoRepository,
                tagRepository = tagRepository,
                currentTimeProvider = MutableTimeProvider(TimestampMillis(today + 1))
            ),
            formatMemoTextUseCase = FormatMemoTextUseCase()
        )
    }

    private class FailingMemoRepository(private val throwable: Throwable) : MemoRepository {

        override fun observeActiveMemos(): Flow<List<Memo>> = flow {
            throw throwable
        }

        override fun observeActiveMemosBySearchQuery(query: SearchQuery): Flow<List<Memo>> = flow {
            throw throwable
        }

        override fun observeActiveMemosCreatedBetween(
            from: TimestampMillis,
            to: TimestampMillis
        ): Flow<List<Memo>> = flow { throw throwable }

        override fun observeTrashedMemos(): Flow<List<Memo>> = flowOf(emptyList())

        override suspend fun getActiveMemo(id: MemoId): Memo? = null

        override suspend fun saveMemo(memo: Memo) = Unit

        override suspend fun moveMemoToTrash(id: MemoId, deletedAt: TimestampMillis) = Unit

        override suspend fun restoreMemoFromTrash(id: MemoId) = Unit

        override suspend fun deleteMemoPermanently(id: MemoId) = Unit

        override suspend fun deleteTrashedMemosDeletedAtOrBefore(cutoff: TimestampMillis) = Unit

        override suspend fun getAllActiveMemos(): List<Memo> = emptyList()

        override suspend fun saveAllMemos(memos: List<Memo>) = Unit
    }

    private class SaveFailingMemoRepository(private val memo: Memo) : MemoRepository {

        override fun observeActiveMemos(): Flow<List<Memo>> = flowOf(listOf(memo))

        override fun observeActiveMemosBySearchQuery(query: SearchQuery): Flow<List<Memo>> =
            flowOf(emptyList())

        override fun observeActiveMemosCreatedBetween(
            from: TimestampMillis,
            to: TimestampMillis
        ): Flow<List<Memo>> = flowOf(emptyList())

        override fun observeTrashedMemos(): Flow<List<Memo>> = flowOf(emptyList())

        override suspend fun getActiveMemo(id: MemoId): Memo? = memo.takeIf { it.id == id }

        override suspend fun saveMemo(memo: Memo): Unit =
            throw IllegalStateException("Failed to save memo.")

        override suspend fun moveMemoToTrash(id: MemoId, deletedAt: TimestampMillis) = Unit

        override suspend fun restoreMemoFromTrash(id: MemoId) = Unit

        override suspend fun deleteMemoPermanently(id: MemoId) = Unit

        override suspend fun deleteTrashedMemosDeletedAtOrBefore(cutoff: TimestampMillis) = Unit

        override suspend fun getAllActiveMemos(): List<Memo> = emptyList()

        override suspend fun saveAllMemos(memos: List<Memo>): Unit =
            throw IllegalStateException("Failed to save memos.")
    }
}
