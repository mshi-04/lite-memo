package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.appvoyager.litememo.domain.FakeMemoEditDraftRepository
import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.FakeTagRepository
import com.appvoyager.litememo.domain.MutableTimeProvider
import com.appvoyager.litememo.domain.QueueMemoIdProvider
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.MemoEditDraft
import com.appvoyager.litememo.domain.model.MemoEditDraftTarget
import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.MemoTitle
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.tagFixture
import com.appvoyager.litememo.domain.usecase.ClearMemoEditDraftUseCase
import com.appvoyager.litememo.domain.usecase.FormatMemoTextUseCase
import com.appvoyager.litememo.domain.usecase.GetMemoEditDraftUseCase
import com.appvoyager.litememo.domain.usecase.GetMemoUseCase
import com.appvoyager.litememo.domain.usecase.MoveMemoToTrashUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTagsUseCase
import com.appvoyager.litememo.domain.usecase.SaveMemoEditDraftUseCase
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
    fun uiStateRestoresSavedStateDraft() = runTest(dispatcher) {
        // Arrange
        val viewModel = memoEditViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    "draftTitle" to "Saved title",
                    "draftBody" to "Saved body",
                    "draftTagIds" to arrayListOf("tag-1"),
                    "draftIsFavorite" to true
                )
            )
        )
        advanceUntilIdle()

        // Act
        val state = viewModel.uiState.value

        // Assert
        assertEquals(
            MemoEditDraftSnapshot("Saved title", "Saved body", setOf("tag-1"), true),
            MemoEditDraftSnapshot(state.title, state.body, state.selectedTagIds, state.isFavorite)
        )
    }

    @Test
    fun uiStateRestoresStoredDraft() = runTest(dispatcher) {
        // Arrange
        val draft = memoEditDraft(
            title = "Stored title",
            body = "Stored body",
            tagIds = listOf(TagId("tag-1")),
            isFavorite = true
        )
        val viewModel = memoEditViewModel(
            draftRepository = FakeMemoEditDraftRepository(listOf(draft))
        )
        advanceUntilIdle()

        // Act
        val state = viewModel.uiState.value

        // Assert
        assertEquals(
            MemoEditDraftSnapshot("Stored title", "Stored body", setOf("tag-1"), true),
            MemoEditDraftSnapshot(state.title, state.body, state.selectedTagIds, state.isFavorite)
        )
    }

    @Test
    fun normalUiStateLoadsExistingMemoWhenNoDraftExists() = runTest(dispatcher) {
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
        advanceUntilIdle()
        val state = viewModel.uiState.value

        // Assert
        assertEquals(
            false to MemoEditDraftSnapshot("Existing title", "Existing body", setOf("tag-1"), true),
            state.isLoading to
                MemoEditDraftSnapshot(
                    state.title,
                    state.body,
                    state.selectedTagIds,
                    state.isFavorite
                )
        )
    }

    @Test
    fun updateTitleSavesDraftAfterDebounce() = runTest(dispatcher) {
        // Arrange
        val draftRepository = FakeMemoEditDraftRepository()
        val viewModel = memoEditViewModel(draftRepository = draftRepository)
        advanceUntilIdle()

        // Act
        viewModel.updateTitle("Draft")
        advanceTimeBy(1_000L.milliseconds)
        advanceUntilIdle()

        // Assert
        assertEquals(
            MemoTitle("Draft"),
            draftRepository.savedDrafts.single().title
        )
    }

    @Test
    fun coroutineRapidTitleChangesSaveOnlyLatestDraftAfterDebounce() = runTest(dispatcher) {
        // Arrange
        val draftRepository = FakeMemoEditDraftRepository()
        val viewModel = memoEditViewModel(draftRepository = draftRepository)
        advanceUntilIdle()

        // Act
        // Coroutine/Boundary: a new edit before debounce cancels the previous autosave.
        viewModel.updateTitle("First")
        advanceTimeBy(500L.milliseconds)
        viewModel.updateTitle("Second")
        advanceTimeBy(1_000L.milliseconds)
        advanceUntilIdle()

        // Assert
        assertEquals(listOf(MemoTitle("Second")), draftRepository.savedDrafts.map { it.title })
    }

    @Test
    fun requestBackFlushesDraft() = runTest(dispatcher) {
        // Arrange
        val draftRepository = FakeMemoEditDraftRepository()
        val viewModel = memoEditViewModel(draftRepository = draftRepository)
        advanceUntilIdle()

        // Act
        viewModel.updateTitle("Draft")
        viewModel.requestBack()
        advanceUntilIdle()

        // Assert
        assertEquals(
            MemoTitle("Draft"),
            draftRepository.savedDrafts.single().title
        )
    }

    @Test
    fun saveClearsDraftWhenMemoIsSaved() = runTest(dispatcher) {
        // Arrange
        val target = MemoEditDraftTarget.newMemo(null)
        val seededDraft = memoEditDraft(target = target, title = "Seeded")
        val draftRepository = FakeMemoEditDraftRepository(listOf(seededDraft))
        val viewModel = memoEditViewModel(draftRepository = draftRepository)
        advanceUntilIdle()

        // Act
        viewModel.updateTitle("Title")
        viewModel.save()
        advanceUntilIdle()

        // Assert
        assertEquals(listOf(target), draftRepository.clearedTargets)
        assertEquals(emptyList<MemoEditDraft>(), draftRepository.currentDrafts())
    }

    @Test
    fun deleteClearsDraftWhenMemoIsDeleted() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(id = "memo-1")
        val target = MemoEditDraftTarget.existingMemo(memo.id)
        val seededDraft = memoEditDraft(target = target, title = "Seeded")
        val draftRepository = FakeMemoEditDraftRepository(listOf(seededDraft))
        val viewModel = memoEditViewModel(
            memo = memo,
            draftRepository = draftRepository
        )
        advanceUntilIdle()

        // Act
        viewModel.delete()
        advanceUntilIdle()

        // Assert
        assertEquals(listOf(target), draftRepository.clearedTargets)
        assertEquals(emptyList<MemoEditDraft>(), draftRepository.currentDrafts())
    }

    @Test
    fun blankDraftClearsDraftAfterDebounce() = runTest(dispatcher) {
        // Arrange
        val draftRepository = FakeMemoEditDraftRepository()
        val viewModel = memoEditViewModel(draftRepository = draftRepository)
        advanceUntilIdle()

        // Act
        viewModel.updateTitle(" ")
        advanceTimeBy(1_000L.milliseconds)
        advanceUntilIdle()

        // Assert
        assertEquals(listOf(MemoEditDraftTarget.newMemo(null)), draftRepository.clearedTargets)
    }

    @Test
    fun flowSaveDoesNotNavigateBackWhenBlankDraftClearFails() = runTest(dispatcher) {
        // Arrange
        val draftRepository = FakeMemoEditDraftRepository(clearDraftError = IllegalStateException())
        val viewModel = memoEditViewModel(draftRepository = draftRepository)
        advanceUntilIdle()

        // Act & Assert
        viewModel.navigationEvent.test {
            viewModel.updateTitle(" ")
            viewModel.save()
            advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun flowSaveEmitsDraftErrorWhenBlankDraftClearFails() = runTest(dispatcher) {
        // Arrange
        val draftRepository = FakeMemoEditDraftRepository(clearDraftError = IllegalStateException())
        val viewModel = memoEditViewModel(draftRepository = draftRepository)
        advanceUntilIdle()

        // Act & Assert
        viewModel.draftErrorEvent.test {
            viewModel.updateTitle(" ")
            viewModel.save()
            advanceUntilIdle()
            assertEquals(Unit, awaitItem())
        }
    }

    @Test
    fun flowSaveNavigatesBackWhenDraftClearFailsAfterMemoIsSaved() = runTest(dispatcher) {
        // Arrange
        val draftRepository = FakeMemoEditDraftRepository(clearDraftError = IllegalStateException())
        val viewModel = memoEditViewModel(draftRepository = draftRepository)
        advanceUntilIdle()

        // Act & Assert
        viewModel.navigationEvent.test {
            viewModel.updateTitle("Title")
            viewModel.save()
            advanceUntilIdle()
            assertEquals(MemoEditNavigationEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun flowSaveEmitsDraftErrorWhenDraftClearFailsAfterMemoIsSaved() = runTest(dispatcher) {
        // Arrange
        val draftRepository = FakeMemoEditDraftRepository(clearDraftError = IllegalStateException())
        val viewModel = memoEditViewModel(draftRepository = draftRepository)
        advanceUntilIdle()

        // Act & Assert
        viewModel.draftErrorEvent.test {
            viewModel.updateTitle("Title")
            viewModel.save()
            advanceUntilIdle()
            assertEquals(Unit, awaitItem())
        }
    }

    @Test
    fun flowDeleteEmitsMemoDeletedEventWithDeletedMemo() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(id = "memo-1")
        val viewModel = memoEditViewModel(
            memo = memo
        )
        advanceUntilIdle()

        // Act & Assert
        viewModel.navigationEvent.test {
            viewModel.delete()
            advanceUntilIdle()
            assertEquals(MemoEditNavigationEvent.MemoDeleted(memo.id), awaitItem())
        }
    }

    @Test
    fun flowDeleteEmitsMemoDeletedEventWhenDraftClearFails() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(id = "memo-1")
        val draftRepository = FakeMemoEditDraftRepository(clearDraftError = IllegalStateException())
        val viewModel = memoEditViewModel(
            memo = memo,
            draftRepository = draftRepository
        )
        advanceUntilIdle()

        // Act & Assert
        viewModel.navigationEvent.test {
            viewModel.delete()
            advanceUntilIdle()
            assertEquals(MemoEditNavigationEvent.MemoDeleted(memo.id), awaitItem())
        }
    }

    @Test
    fun flowDeleteEmitsDraftErrorWhenDraftClearFails() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(id = "memo-1")
        val draftRepository = FakeMemoEditDraftRepository(clearDraftError = IllegalStateException())
        val viewModel = memoEditViewModel(
            memo = memo,
            draftRepository = draftRepository
        )
        advanceUntilIdle()

        // Act & Assert
        viewModel.draftErrorEvent.test {
            viewModel.delete()
            advanceUntilIdle()
            assertEquals(Unit, awaitItem())
        }
    }

    @Test
    fun flowSaveEmitsOperationErrorWhenMemoSaveFails() = runTest(dispatcher) {
        // Arrange
        val viewModel = memoEditViewModel(
            memoRepository = SaveFailingMemoRepository()
        )
        advanceUntilIdle()

        // Act & Assert
        // Flow/Error: save repository failure emits a SaveFailed operation event.
        viewModel.operationErrorEvent.test {
            viewModel.updateTitle("Title")
            viewModel.save()
            advanceUntilIdle()
            assertEquals(MemoEditOperationErrorEvent.SaveFailed, awaitItem())
        }
    }

    @Test
    fun flowDeleteEmitsOperationErrorWhenMoveToTrashFails() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(id = "memo-1")
        val viewModel = memoEditViewModel(
            memo = memo,
            memoRepository = MoveToTrashFailingMemoRepository(memo)
        )
        advanceUntilIdle()

        // Act & Assert
        // Flow/Error/StateTransition: delete failure emits an event and clears pending state.
        viewModel.operationErrorEvent.test {
            viewModel.delete()
            advanceUntilIdle()
            assertEquals(
                MemoEditOperationErrorEvent.DeleteFailed to false,
                awaitItem() to viewModel.uiState.value.isDeletePending
            )
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
        draftRepository: FakeMemoEditDraftRepository = FakeMemoEditDraftRepository(),
        memoRepository: MemoRepository = FakeMemoRepository(listOfNotNull(memo))
    ): MemoEditViewModel {
        val tagRepository = FakeTagRepository(
            listOf(tagFixture(id = "tag-1"))
        )
        return MemoEditViewModel(
            savedStateHandle = savedStateHandle,
            getMemoUseCase = GetMemoUseCase(memoRepository),
            saveMemoUseCase = SaveMemoUseCase(
                memoRepository = memoRepository,
                tagRepository = tagRepository,
                memoIdProvider = QueueMemoIdProvider(listOf(MemoId("generated-id"))),
                currentTimeProvider = MutableTimeProvider(TimestampMillis(2000L))
            ),
            moveMemoToTrashUseCase = MoveMemoToTrashUseCase(
                memoRepository = memoRepository,
                currentTimeProvider = MutableTimeProvider(TimestampMillis(2_000L))
            ),
            observeTagsUseCase = ObserveTagsUseCase(tagRepository),
            getMemoEditDraftUseCase = GetMemoEditDraftUseCase(draftRepository),
            saveMemoEditDraftUseCase = SaveMemoEditDraftUseCase(draftRepository),
            clearMemoEditDraftUseCase = ClearMemoEditDraftUseCase(draftRepository),
            formatMemoTextUseCase = FormatMemoTextUseCase()
        )
    }

    private fun memoEditDraft(
        target: MemoEditDraftTarget = MemoEditDraftTarget.newMemo(null),
        title: String = "",
        body: String = "",
        createdAt: TimestampMillis? = null,
        tagIds: List<TagId> = emptyList(),
        isFavorite: Boolean = false
    ) = MemoEditDraft(
        target = target,
        title = MemoTitle(title),
        body = MemoBody(body),
        createdAt = createdAt,
        tagIds = tagIds,
        isFavorite = isFavorite
    )

    private class SaveFailingMemoRepository : MemoRepository by FakeMemoRepository() {
        override suspend fun saveMemo(memo: Memo): Unit = error("Failed to save memo.")
    }

    private class MoveToTrashFailingMemoRepository(memo: Memo) :
        MemoRepository by FakeMemoRepository(listOf(memo)) {
        override suspend fun moveMemoToTrash(id: MemoId, deletedAt: TimestampMillis): Unit =
            error("Failed to move memo to trash.")
    }
}

private data class MemoEditDraftSnapshot(
    val title: String,
    val body: String,
    val selectedTagIds: Set<String>,
    val isFavorite: Boolean
)
