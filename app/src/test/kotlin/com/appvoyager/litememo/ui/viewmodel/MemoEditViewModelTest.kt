package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.FakeTagRepository
import com.appvoyager.litememo.domain.MutableTimeProvider
import com.appvoyager.litememo.domain.QueueMemoIdProvider
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.tagFixture
import com.appvoyager.litememo.domain.usecase.DiscardMemoUseCase
import com.appvoyager.litememo.domain.usecase.FormatMemoTextUseCase
import com.appvoyager.litememo.domain.usecase.GenerateMemoIdUseCase
import com.appvoyager.litememo.domain.usecase.GetMemoUseCase
import com.appvoyager.litememo.domain.usecase.MoveMemoToTrashUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTagsUseCase
import com.appvoyager.litememo.domain.usecase.SaveMemoUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class MemoEditViewModelTest {

    private lateinit var dispatcher: TestDispatcher

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
    fun normalUiStateRestoresSavedStateEdit() = runTest(dispatcher) {
        // Arrange
        val viewModel = memoEditViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    "editTitle" to "Saved title",
                    "editBody" to "Saved body",
                    "editTagIds" to arrayListOf("tag-1"),
                    "editIsFavorite" to true
                )
            )
        )
        advanceUntilIdle()

        // Act
        // Normal: SavedStateHandle edits win over database and empty defaults.
        val state = viewModel.uiState.value

        // Assert
        assertEquals(
            MemoEditSnapshot("Saved title", "Saved body", setOf("tag-1"), true),
            MemoEditSnapshot(state.title, state.body, state.selectedTagIds, state.isFavorite)
        )
    }

    @Test
    fun normalUiStateLoadsExistingMemo() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(
            id = "memo-1",
            title = "Existing title",
            body = "Existing body",
            tagIds = listOf(TagId("tag-1")),
            isFavorite = true
        )
        val viewModel = memoEditViewModel(memo = memo)

        // Act
        // Normal: existing memo edit starts from Room.
        advanceUntilIdle()
        val state = viewModel.uiState.value

        // Assert
        assertEquals(
            false to MemoEditSnapshot("Existing title", "Existing body", setOf("tag-1"), true),
            state.isLoading to MemoEditSnapshot(
                state.title,
                state.body,
                state.selectedTagIds,
                state.isFavorite
            )
        )
    }

    @Test
    fun normalAutosavePersistsAfterDebounce() = runTest(dispatcher) {
        // Arrange
        val memoRepository = FakeMemoRepository()
        val viewModel = memoEditViewModel(memoRepository = memoRepository)
        advanceUntilIdle()

        // Act
        // Normal: non-empty edits are saved silently after debounce.
        viewModel.updateTitle("Autosaved")
        advanceTimeBy(1_000L.milliseconds)
        advanceUntilIdle()

        // Assert
        assertEquals("Autosaved", memoRepository.savedMemos.single().title.value)
    }

    @Test
    fun coroutineRapidEditsPersistOnlyLatestValue() = runTest(dispatcher) {
        // Arrange
        val memoRepository = FakeMemoRepository()
        val viewModel = memoEditViewModel(memoRepository = memoRepository)
        advanceUntilIdle()

        // Act
        // Coroutine: a new edit before debounce cancels the previous autosave.
        viewModel.updateTitle("First")
        advanceTimeBy(500L.milliseconds)
        viewModel.updateTitle("Second")
        advanceTimeBy(1_000L.milliseconds)
        advanceUntilIdle()

        // Assert
        assertEquals(listOf("Second"), memoRepository.savedMemos.map { it.title.value })
    }

    @Test
    fun coroutineBlankContentIsNeverAutosaved() = runTest(dispatcher) {
        // Arrange
        val memoRepository = FakeMemoRepository()
        val viewModel = memoEditViewModel(memoRepository = memoRepository)
        advanceUntilIdle()

        // Act
        // Coroutine: blank title and body do not create a Room row.
        viewModel.updateTitle(" ")
        advanceTimeBy(1_000L.milliseconds)
        advanceUntilIdle()

        // Assert
        assertEquals(emptyList<Memo>(), memoRepository.savedMemos)
    }

    @Test
    fun stateTransitionAutosaveReusesSameMemoIdWithinSession() = runTest(dispatcher) {
        // Arrange
        val memoRepository = FakeMemoRepository()
        val viewModel = memoEditViewModel(memoRepository = memoRepository)
        advanceUntilIdle()

        // Act
        // StateTransition: generated id is fixed at editor session start.
        viewModel.updateTitle("Title")
        advanceTimeBy(1_000L.milliseconds)
        advanceUntilIdle()
        viewModel.updateBody("Body")
        advanceTimeBy(1_000L.milliseconds)
        advanceUntilIdle()

        // Assert
        assertEquals(
            listOf(MemoId("generated-id")),
            memoRepository.savedMemos
                .map { it.id }
                .distinct()
        )
    }

    @Test
    fun normalExistingMemoAutosaveUsesNavigationMemoId() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(id = "memo-1")
        val memoRepository = FakeMemoRepository(listOf(memo))
        val viewModel = memoEditViewModel(memo = memo, memoRepository = memoRepository)
        advanceUntilIdle()

        // Act
        // Normal: existing memo updates keep the navigation argument id.
        viewModel.updateTitle("Updated")
        advanceTimeBy(1_000L.milliseconds)
        advanceUntilIdle()

        // Assert
        assertEquals(MemoId("memo-1"), memoRepository.savedMemos.single().id)
    }

    @Test
    fun flowBlankNewMemoFinishNavigatesBackWithoutRow() = runTest(dispatcher) {
        // Arrange
        val memoRepository = FakeMemoRepository()
        val viewModel = memoEditViewModel(memoRepository = memoRepository)
        advanceUntilIdle()

        // Act & Assert
        // Flow: empty new memo is discarded silently.
        viewModel.navigationEvent.test {
            viewModel.finishEditing()
            advanceUntilIdle()
            assertEquals(
                MemoEditNavigationEvent.NavigateBack to emptyList<MemoId>(),
                awaitItem() to memoRepository.currentMemos().map { it.id }
            )
        }
    }

    @Test
    fun flowPersistedNewMemoEmptiedOnFinishIsHardDeleted() = runTest(dispatcher) {
        // Arrange
        val memoRepository = FakeMemoRepository()
        val viewModel = memoEditViewModel(memoRepository = memoRepository)
        advanceUntilIdle()
        viewModel.updateTitle("Title")
        advanceTimeBy(1_000L.milliseconds)
        advanceUntilIdle()

        // Act & Assert
        // Flow: a new memo that becomes blank before leaving is removed from Room.
        viewModel.navigationEvent.test {
            viewModel.updateTitle("")
            viewModel.finishEditing()
            advanceUntilIdle()
            assertEquals(
                MemoEditNavigationEvent.NavigateBack to emptyList<MemoId>(),
                awaitItem() to memoRepository.currentMemos().map { it.id }
            )
        }
    }

    @Test
    fun flowRestoredNewMemoEmptiedOnFinishIsStillHardDeleted() = runTest(dispatcher) {
        // Arrange
        val memoRepository = FakeMemoRepository(listOf(memoFixture(id = "generated-id")))
        val viewModel = memoEditViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    "memoId" to "generated-id",
                    "generatedMemoId" to "generated-id",
                    "editTitle" to "",
                    "editBody" to ""
                )
            ),
            memoRepository = memoRepository
        )
        advanceUntilIdle()

        // Act & Assert
        // Flow: restored sessions that started as new still discard instead of trashing.
        viewModel.navigationEvent.test {
            viewModel.finishEditing()
            advanceUntilIdle()
            assertEquals(
                RestoredNewMemoFinishSnapshot(
                    navigationEvent = MemoEditNavigationEvent.NavigateBack,
                    remainingMemoIds = emptyList(),
                    movedToTrashIds = emptyList()
                ),
                RestoredNewMemoFinishSnapshot(
                    navigationEvent = awaitItem(),
                    remainingMemoIds = memoRepository.currentMemos().map { it.id },
                    movedToTrashIds = memoRepository.movedToTrash.map { it.memoId }
                )
            )
        }
    }

    @Test
    fun flowExistingMemoEmptiedOnFinishMovesToTrash() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(id = "memo-1")
        val memoRepository = FakeMemoRepository(listOf(memo))
        val viewModel = memoEditViewModel(memo = memo, memoRepository = memoRepository)
        advanceUntilIdle()

        // Act & Assert
        // Flow: blanking an existing memo is treated as deletion with undo support.
        viewModel.navigationEvent.test {
            viewModel.updateTitle("")
            viewModel.updateBody("")
            viewModel.finishEditing()
            advanceUntilIdle()
            assertEquals(MemoEditNavigationEvent.MemoDeleted(memo.id), awaitItem())
        }
    }

    @Test
    fun flowFinishFlushesPendingEditBeforeNavigateBack() = runTest(dispatcher) {
        // Arrange
        val memoRepository = FakeMemoRepository()
        val viewModel = memoEditViewModel(memoRepository = memoRepository)
        advanceUntilIdle()

        // Act & Assert
        // Flow: explicit back persists pending edits without waiting for debounce.
        viewModel.navigationEvent.test {
            viewModel.updateTitle("Pending")
            viewModel.finishEditing()
            advanceUntilIdle()
            assertEquals(
                MemoEditNavigationEvent.NavigateBack to "Pending",
                awaitItem() to memoRepository.savedMemos.single().title.value
            )
        }
    }

    @Test
    fun coroutineFinishEditingIgnoresLaterTextChanges() = runTest(dispatcher) {
        // Arrange
        val memoRepository = FakeMemoRepository()
        val viewModel = memoEditViewModel(memoRepository = memoRepository)
        advanceUntilIdle()

        // Act
        // Coroutine: once finish starts, late UI updates must not re-arm autosave.
        viewModel.updateTitle("Pending")
        viewModel.finishEditing()
        viewModel.updateBody("Late body")
        advanceTimeBy(1_000L.milliseconds)
        advanceUntilIdle()

        // Assert
        assertEquals(
            MemoEditSnapshot("Pending", "", emptySet(), false),
            memoRepository.savedMemos.single().let { memo ->
                MemoEditSnapshot(
                    title = memo.title.value,
                    body = memo.body.value,
                    selectedTagIds = memo.tagIds.map { it.value }.toSet(),
                    isFavorite = memo.isFavorite
                )
            }
        )
    }

    @Test
    fun flowFinishSaveFailureKeepsDraftWithoutNavigating() = runTest(dispatcher) {
        // Arrange
        val savedStateHandle = SavedStateHandle()
        val viewModel = memoEditViewModel(
            savedStateHandle = savedStateHandle,
            memoRepository = SaveFailingMemoRepository()
        )
        advanceUntilIdle()

        // Act & Assert
        // Flow/Error: finish-time save failure keeps the draft and does not navigate away.
        viewModel.navigationEvent.test {
            viewModel.updateTitle("Unsaved")
            viewModel.finishEditing()
            advanceUntilIdle()
            expectNoEvents()
        }

        // Assert: the edited draft survives in SavedStateHandle for a later retry.
        assertEquals("Unsaved", savedStateHandle.get<String>("editTitle"))
    }

    @Test
    fun coroutineFlushEditsPersistsWithoutNavigation() = runTest(dispatcher) {
        // Arrange
        val memoRepository = FakeMemoRepository()
        val viewModel = memoEditViewModel(memoRepository = memoRepository)
        advanceUntilIdle()

        // Act
        // Coroutine: ON_STOP flush saves but does not close the editor.
        viewModel.updateTitle("Background")
        viewModel.flushEdits()
        advanceUntilIdle()

        // Assert
        assertEquals("Background", memoRepository.savedMemos.single().title.value)
    }

    @Test
    fun coroutineDeleteCancelsPendingAutosave() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(id = "memo-1")
        val memoRepository = FakeMemoRepository(listOf(memo))
        val viewModel = memoEditViewModel(memo = memo, memoRepository = memoRepository)
        advanceUntilIdle()

        // Act
        // Coroutine: delete prevents delayed autosave from recreating the memo.
        viewModel.updateTitle("Pending")
        viewModel.delete()
        viewModel.updateBody("Late body")
        advanceTimeBy(1_000L.milliseconds)
        advanceUntilIdle()

        // Assert
        assertEquals(emptyList<Memo>(), memoRepository.savedMemos)
    }

    @Test
    fun flowAutosaveFailureEmitsSaveFailed() = runTest(dispatcher) {
        // Arrange
        val viewModel = memoEditViewModel(memoRepository = SaveFailingMemoRepository())
        advanceUntilIdle()

        // Act & Assert
        // Flow/Error: autosave failure is surfaced as the existing save error event.
        viewModel.operationErrorEvent.test {
            viewModel.updateTitle("Title")
            advanceTimeBy(1_000L.milliseconds)
            advanceUntilIdle()
            assertEquals(MemoEditOperationErrorEvent.SaveFailed, awaitItem())
        }
    }

    @Test
    fun flowRapidFinishEditingNavigatesBackOnce() = runTest(dispatcher) {
        // Arrange
        val viewModel = memoEditViewModel()
        advanceUntilIdle()

        // Act & Assert
        // Flow: finishing guard prevents duplicate navigation events.
        viewModel.navigationEvent.test {
            viewModel.updateTitle("Title")
            viewModel.finishEditing()
            viewModel.finishEditing()
            advanceUntilIdle()
            assertEquals(MemoEditNavigationEvent.NavigateBack, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun flowDeleteEmitsMemoDeletedEventWithDeletedMemo() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(id = "memo-1")
        val viewModel = memoEditViewModel(memo = memo)
        advanceUntilIdle()

        // Act & Assert
        // Flow: toolbar delete keeps the existing delete event contract.
        viewModel.navigationEvent.test {
            viewModel.delete()
            advanceUntilIdle()
            assertEquals(MemoEditNavigationEvent.MemoDeleted(memo.id), awaitItem())
        }
    }

    @Test
    fun flowDeleteFailureEmitsOperationError() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(id = "memo-1")
        val viewModel = memoEditViewModel(
            memo = memo,
            memoRepository = MoveToTrashFailingMemoRepository(memo)
        )
        advanceUntilIdle()

        // Act & Assert
        // Flow/Error: delete failure emits the existing delete error event.
        viewModel.operationErrorEvent.test {
            viewModel.delete()
            advanceUntilIdle()
            assertEquals(MemoEditOperationErrorEvent.DeleteFailed, awaitItem())
        }
    }

    private fun memoEditViewModel(
        memo: Memo? = null,
        savedStateHandle: SavedStateHandle = memo?.let {
            SavedStateHandle(
                mapOf(
                    "memoId" to it.id.value,
                    "createdAt" to it.createdAt.value
                )
            )
        } ?: SavedStateHandle(),
        memoRepository: MemoRepository = FakeMemoRepository(listOfNotNull(memo))
    ): MemoEditViewModel {
        val tagRepository = FakeTagRepository(listOf(tagFixture(id = "tag-1")))
        val timeProvider = MutableTimeProvider(TimestampMillis(2_000L))
        return MemoEditViewModel(
            savedStateHandle = savedStateHandle,
            getMemoUseCase = GetMemoUseCase(memoRepository),
            saveMemoUseCase = SaveMemoUseCase(
                memoRepository = memoRepository,
                tagRepository = tagRepository,
                memoIdProvider = QueueMemoIdProvider(listOf(MemoId("unused-id"))),
                currentTimeProvider = timeProvider
            ),
            moveMemoToTrashUseCase = MoveMemoToTrashUseCase(
                memoRepository = memoRepository,
                currentTimeProvider = timeProvider
            ),
            discardMemoUseCase = DiscardMemoUseCase(memoRepository),
            generateMemoIdUseCase = GenerateMemoIdUseCase(
                QueueMemoIdProvider(listOf(MemoId("generated-id")))
            ),
            observeTagsUseCase = ObserveTagsUseCase(tagRepository),
            formatMemoTextUseCase = FormatMemoTextUseCase()
        )
    }

    private class SaveFailingMemoRepository : MemoRepository by FakeMemoRepository() {
        override suspend fun saveMemo(memo: Memo): Unit = error("Failed to save memo.")
    }

    private class MoveToTrashFailingMemoRepository(memo: Memo) :
        MemoRepository by FakeMemoRepository(listOf(memo)) {
        override suspend fun moveMemoToTrash(id: MemoId, deletedAt: TimestampMillis): Unit =
            error("Failed to move memo to trash.")
    }
}

private data class MemoEditSnapshot(
    val title: String,
    val body: String,
    val selectedTagIds: Set<String>,
    val isFavorite: Boolean
)

private data class RestoredNewMemoFinishSnapshot(
    val navigationEvent: MemoEditNavigationEvent,
    val remainingMemoIds: List<MemoId>,
    val movedToTrashIds: List<MemoId>
)
